import fs from "fs/promises";
import path from "path";
import { buildDefaultState } from "./defaults.js";

function isObject(value) {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function mergeDeep(target, source) {
  if (Array.isArray(source)) {
    return source;
  }

  if (isObject(target) && isObject(source)) {
    const merged = { ...target };
    for (const key of Object.keys(source)) {
      merged[key] = mergeDeep(target[key], source[key]);
    }
    return merged;
  }

  return source === undefined ? target : source;
}

function deepClone(value) {
  return JSON.parse(JSON.stringify(value));
}

export class JsonStore {
  #filePath;
  #state = null;
  #writeChain = Promise.resolve();

  constructor(filePath) {
    this.#filePath = filePath;
  }

  async init() {
    if (this.#state !== null) {
      return;
    }

    const defaults = buildDefaultState();

    try {
      const raw = await fs.readFile(this.#filePath, "utf-8");
      const parsed = JSON.parse(raw);
      this.#state = mergeDeep(defaults, parsed);
      await this.#persist();
      return;
    } catch {
      this.#state = defaults;
      await this.#persist();
    }
  }

  async getState() {
    await this.init();
    return deepClone(this.#state);
  }

  async saveState(nextState) {
    await this.init();
    return this.#enqueueWrite(async () => {
      const defaults = buildDefaultState();
      this.#state = mergeDeep(defaults, nextState ?? {});
      await this.#persist();
      return deepClone(this.#state);
    });
  }

  async update(updater) {
    await this.init();
    return this.#enqueueWrite(async () => {
      const current = deepClone(this.#state);
      const updated = await updater(current);
      const defaults = buildDefaultState();
      this.#state = mergeDeep(defaults, updated ?? current);
      await this.#persist();
      return deepClone(this.#state);
    });
  }

  async #enqueueWrite(task) {
    this.#writeChain = this.#writeChain.then(task, task);
    return this.#writeChain;
  }

  async #persist() {
    const dir = path.dirname(this.#filePath);
    await fs.mkdir(dir, { recursive: true });
    await fs.writeFile(this.#filePath, JSON.stringify(this.#state, null, 2), "utf-8");
  }
}
