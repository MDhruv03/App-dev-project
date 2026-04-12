import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useColorScheme } from "react-native";
import { AppTheme, darkTheme, lightTheme, ThemeMode } from "./palette";

type ThemeContextValue = {
  theme: AppTheme;
  mode: ThemeMode;
  isThemeHydrated: boolean;
  toggleMode: () => void;
};

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);
const THEME_STORAGE_KEY = "@madlab/theme-mode:v1";

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const system = useColorScheme();
  const [forcedMode, setForcedMode] = useState<ThemeMode | null>(null);
  const [isThemeHydrated, setIsThemeHydrated] = useState(false);

  const mode: ThemeMode = forcedMode ?? (system === "dark" ? "dark" : "light");
  const theme = mode === "dark" ? darkTheme : lightTheme;

  useEffect(() => {
    let isMounted = true;

    async function hydrateThemePreference() {
      try {
        const savedMode = await AsyncStorage.getItem(THEME_STORAGE_KEY);
        if (!isMounted || !savedMode) {
          return;
        }

        if (savedMode === "light" || savedMode === "dark") {
          setForcedMode(savedMode);
        }
      } finally {
        if (isMounted) {
          setIsThemeHydrated(true);
        }
      }
    }

    void hydrateThemePreference();

    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    if (!isThemeHydrated) {
      return;
    }

    async function persistThemePreference() {
      try {
        if (forcedMode) {
          await AsyncStorage.setItem(THEME_STORAGE_KEY, forcedMode);
        } else {
          await AsyncStorage.removeItem(THEME_STORAGE_KEY);
        }
      } catch {
        // Ignore storage write failures and keep runtime theme responsive.
      }
    }

    void persistThemePreference();
  }, [forcedMode, isThemeHydrated]);

  const value = useMemo<ThemeContextValue>(() => {
    return {
      theme,
      mode,
      isThemeHydrated,
      toggleMode: () => {
        setForcedMode((prev) => {
          const current = prev ?? (system === "dark" ? "dark" : "light");
          return current === "dark" ? "light" : "dark";
        });
      },
    };
  }, [isThemeHydrated, mode, system, theme]);

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useAppTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error("useAppTheme must be used inside ThemeProvider");
  }
  return context;
}
