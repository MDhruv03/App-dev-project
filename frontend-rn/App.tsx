import "react-native-gesture-handler";
import React, { useState } from "react";
import { View } from "react-native";
import { StatusBar } from "expo-status-bar";
import { Feather } from "@expo/vector-icons";
import { NavigationContainer } from "@react-navigation/native";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import {
  useFonts,
  SpaceGrotesk_400Regular,
  SpaceGrotesk_500Medium,
  SpaceGrotesk_700Bold,
} from "@expo-google-fonts/space-grotesk";
import { CormorantGaramond_700Bold } from "@expo-google-fonts/cormorant-garamond";
import { ThemeProvider, useAppTheme } from "./src/theme/ThemeProvider";
import { ThemedText } from "./src/components/ThemedText";
import { DashboardScreen } from "./src/screens/DashboardScreen";
import { TrackerScreen } from "./src/screens/TrackerScreen";
import { CodingScreen } from "./src/screens/CodingScreen";
import { InterviewScreen } from "./src/screens/InterviewScreen";
import { ProfileScreen } from "./src/screens/ProfileScreen";
import { AnalyticsScreen } from "./src/screens/AnalyticsScreen";
import { RoadmapScreen } from "./src/screens/RoadmapScreen";
import { AppStateProvider, useAppState } from "./src/state/AppState";
import { AppBootScreen } from "./src/components/AppBootScreen";
import { AppErrorBoundary } from "./src/components/AppErrorBoundary";

type TabParamList = {
  Discover: undefined;
  Tracker: undefined;
  Coding: undefined;
  Interview: undefined;
  Analytics: undefined;
  Roadmap: undefined;
  Profile: undefined;
};

const Tab = createBottomTabNavigator<TabParamList>();

function AppRoot() {
  const { isHydrated } = useAppState();
  const { isThemeHydrated } = useAppTheme();

  if (!isHydrated || !isThemeHydrated) {
    return (
      <AppBootScreen
        title="Finalizing app state"
        message="Restoring your profile, coding stats, and interview history..."
      />
    );
  }

  return <AppTabs />;
}

function AppTabs() {
  const { theme, mode } = useAppTheme();

  return (
    <>
      <StatusBar style={mode === "dark" ? "light" : "dark"} />
      <NavigationContainer
        theme={{
          dark: mode === "dark",
          colors: {
            primary: theme.colors.accent,
            background: theme.colors.bgBottom,
            card: theme.colors.tabBar,
            text: theme.colors.text,
            border: theme.colors.border,
            notification: theme.colors.danger,
          },
          fonts: {
            regular: {
              fontFamily: "SpaceGrotesk_400Regular",
              fontWeight: "400",
            },
            medium: {
              fontFamily: "SpaceGrotesk_500Medium",
              fontWeight: "500",
            },
            bold: {
              fontFamily: "SpaceGrotesk_700Bold",
              fontWeight: "700",
            },
            heavy: {
              fontFamily: "SpaceGrotesk_700Bold",
              fontWeight: "700",
            },
          },
        }}
      >
        <Tab.Navigator
          screenOptions={({ route }) => ({
            headerShown: false,
            tabBarStyle: {
              height: 84,
              paddingTop: 8,
              paddingBottom: 16,
              backgroundColor: theme.colors.tabBar,
              borderTopColor: theme.colors.border,
              borderTopWidth: 1,
            },
            tabBarLabel: ({ focused }) => (
              <ThemedText
                variant="body"
                strong={focused}
                style={{
                  fontSize: 11,
                  color: focused ? theme.colors.accent : theme.colors.textMuted,
                  marginTop: 1,
                }}
              >
                {route.name}
              </ThemedText>
            ),
            tabBarIcon: ({ focused, size }) => {
              const iconName: Record<keyof TabParamList, keyof typeof Feather.glyphMap> = {
                Discover: "compass",
                Tracker: "clipboard",
                Coding: "code",
                Interview: "mic",
                Analytics: "bar-chart-2",
                Roadmap: "map",
                Profile: "user",
              };

              return (
                <View
                  style={{
                    width: 34,
                    height: 34,
                    borderRadius: 10,
                    alignItems: "center",
                    justifyContent: "center",
                    backgroundColor: focused ? theme.colors.accentSoft : "transparent",
                  }}
                >
                  <Feather
                    name={iconName[route.name as keyof TabParamList]}
                    size={size}
                    color={focused ? theme.colors.accent : theme.colors.textMuted}
                  />
                </View>
              );
            },
          })}
        >
          <Tab.Screen name="Discover" component={DashboardScreen} />
          <Tab.Screen name="Tracker" component={TrackerScreen} />
          <Tab.Screen name="Coding" component={CodingScreen} />
          <Tab.Screen name="Interview" component={InterviewScreen} />
          <Tab.Screen name="Analytics" component={AnalyticsScreen} />
          <Tab.Screen name="Roadmap" component={RoadmapScreen} />
          <Tab.Screen name="Profile" component={ProfileScreen} />
        </Tab.Navigator>
      </NavigationContainer>
    </>
  );
}

export default function App() {
  const [fontsLoaded] = useFonts({
    SpaceGrotesk_400Regular,
    SpaceGrotesk_500Medium,
    SpaceGrotesk_700Bold,
    CormorantGaramond_700Bold,
  });
  const [boundaryVersion, setBoundaryVersion] = useState(0);

  if (!fontsLoaded) {
    return (
      <AppBootScreen
        title="Loading visual system"
        message="Preparing fonts, motion layers, and navigation shell..."
      />
    );
  }

  return (
    <ThemeProvider>
      <AppErrorBoundary
        key={boundaryVersion}
        onReset={() => {
          setBoundaryVersion((prev) => prev + 1);
        }}
      >
        <AppStateProvider>
          <AppRoot />
        </AppStateProvider>
      </AppErrorBoundary>
    </ThemeProvider>
  );
}
