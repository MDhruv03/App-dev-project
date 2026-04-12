import React from "react";
import { Text, TextProps, TextStyle } from "react-native";
import { appFonts } from "../theme/palette";
import { useAppTheme } from "../theme/ThemeProvider";

type Variant = "display" | "title" | "body" | "label";

type Props = TextProps & {
  variant?: Variant;
  strong?: boolean;
  muted?: boolean;
};

export function ThemedText({ variant = "body", strong, muted, style, ...rest }: Props) {
  const { theme } = useAppTheme();

  const variantStyle: Record<Variant, TextStyle> = {
    display: {
      fontFamily: appFonts.display,
      fontSize: 34,
      lineHeight: 38,
      letterSpacing: 0.2,
    },
    title: {
      fontFamily: strong ? appFonts.bodyBold : appFonts.bodyMedium,
      fontSize: 19,
      lineHeight: 24,
      letterSpacing: 0.2,
    },
    body: {
      fontFamily: strong ? appFonts.bodyBold : appFonts.body,
      fontSize: 15,
      lineHeight: 22,
      letterSpacing: 0.1,
    },
    label: {
      fontFamily: strong ? appFonts.bodyBold : appFonts.bodyMedium,
      fontSize: 12,
      lineHeight: 16,
      letterSpacing: 0.5,
      textTransform: "uppercase",
    },
  };

  return (
    <Text
      {...rest}
      style={[
        variantStyle[variant],
        {
          color: muted ? theme.colors.textMuted : theme.colors.text,
        },
        style,
      ]}
    />
  );
}
