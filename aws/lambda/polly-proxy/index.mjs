import { PollyClient, SynthesizeSpeechCommand } from "@aws-sdk/client-polly";

const REGION = process.env.AWS_REGION || "ap-south-1";
const pollyClient = new PollyClient({ region: REGION });

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "content-type",
  "Access-Control-Allow-Methods": "OPTIONS,POST"
};

export const handler = async (event) => {
  const method = event?.requestContext?.http?.method || event?.httpMethod || "POST";

  if (method === "OPTIONS") {
    return {
      statusCode: 200,
      headers: corsHeaders,
      body: ""
    };
  }

  try {
    const body = parseBody(event?.body);
    const text = (body?.text || "").trim();
    const voiceId = (body?.voiceId || "Aditi").trim();
    const engine = (body?.engine || "neural").trim();
    const outputFormat = (body?.outputFormat || "mp3").trim();

    if (!text) {
      return jsonError(400, "text is required");
    }

    if (outputFormat !== "mp3") {
      return jsonError(400, "outputFormat must be mp3");
    }

    const textType = isSsml(text) ? "ssml" : "text";

    const command = new SynthesizeSpeechCommand({
      Text: text,
      TextType: textType,
      VoiceId: voiceId,
      Engine: engine,
      OutputFormat: outputFormat
    });

    const response = await pollyClient.send(command);
    const audioBuffer = await streamToBuffer(response.AudioStream);

    if (!audioBuffer || audioBuffer.length === 0) {
      return jsonError(502, "Polly returned empty audio");
    }

    return {
      statusCode: 200,
      headers: {
        ...corsHeaders,
        "Content-Type": "audio/mpeg"
      },
      isBase64Encoded: true,
      body: audioBuffer.toString("base64")
    };
  } catch (error) {
    console.error("Polly Lambda error", error);
    return jsonError(500, "Polly synthesis failed");
  }
};

function parseBody(body) {
  if (!body) {
    return {};
  }
  if (typeof body === "string") {
    return JSON.parse(body);
  }
  return body;
}

function isSsml(text) {
  const trimmed = text.trim();
  return trimmed.startsWith("<speak") && trimmed.endsWith("</speak>");
}

async function streamToBuffer(stream) {
  if (!stream) {
    return Buffer.alloc(0);
  }

  if (Buffer.isBuffer(stream)) {
    return stream;
  }

  if (stream instanceof Uint8Array) {
    return Buffer.from(stream);
  }

  const chunks = [];
  for await (const chunk of stream) {
    chunks.push(Buffer.from(chunk));
  }
  return Buffer.concat(chunks);
}

function jsonError(statusCode, message) {
  return {
    statusCode,
    headers: {
      ...corsHeaders,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ error: message })
  };
}
