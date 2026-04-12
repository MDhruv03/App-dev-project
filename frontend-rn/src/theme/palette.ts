export type ThemeMode = "light" | "dark";

export type AppTheme = {
  mode: ThemeMode;
  colors: {
    bgTop: string;
    bgMid: string;
    bgBottom: string;
    card: string;
    cardAlt: string;
    border: string;
    text: string;
    textMuted: string;
    accent: string;
    accentSoft: string;
    danger: string;
    success: string;
    warning: string;
    tabBar: string;
  };
  radius: {
    sm: number;
    md: number;
    lg: number;
    xl: number;
  };
  spacing: {
    xs: number;
    sm: number;
    md: number;
    lg: number;
    xl: number;
    xxl: number;
  };
};

const baseRadius = {
  sm: 10,
  md: 16,
  lg: 22,
  xl: 30,
};

const baseSpacing = {
  xs: 6,
  sm: 10,
  md: 16,
  lg: 22,
  xl: 30,
  xxl: 40,
};

export const lightTheme: AppTheme = {
  mode: "light",
  colors: {
    bgTop: "#f4f1e8",
    bgMid: "#ece7da",
    bgBottom: "#dde5e7",
    card: "#fbfaf6",
    cardAlt: "#f2f3ee",
    border: "#d7d4ca",
    text: "#1c2529",
    textMuted: "#5c696f",
    accent: "#0f7a68",
    accentSoft: "#d2eee8",
    danger: "#b84d45",
    success: "#2c7a54",
    warning: "#b77a29",
    tabBar: "#f7f5ee",
  },
  radius: baseRadius,
  spacing: baseSpacing,
};

export const darkTheme: AppTheme = {
  mode: "dark",
  colors: {
    bgTop: "#0d1417",
    bgMid: "#101c20",
    bgBottom: "#15222a",
    card: "#18252d",
    cardAlt: "#1d2f39",
    border: "#2d404d",
    text: "#f0f6f4",
    textMuted: "#a8bbc3",
    accent: "#2dc8ac",
    accentSoft: "#1f4f47",
    danger: "#e2756b",
    success: "#68cc9a",
    warning: "#e1a14a",
    tabBar: "#132128",
  },
  radius: baseRadius,
  spacing: baseSpacing,
};

export const appFonts = {
  display: "CormorantGaramond_700Bold",
  body: "SpaceGrotesk_400Regular",
  bodyMedium: "SpaceGrotesk_500Medium",
  bodyBold: "SpaceGrotesk_700Bold",
};
