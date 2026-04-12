import AsyncStorage from "@react-native-async-storage/async-storage";
import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import {
  fetchCurrentUser,
  loginRequest,
  logoutRequest,
  signUpRequest,
  type AuthUser,
} from "../services/authService";
import { setApiAuthToken } from "../services/apiClient";

type AuthContextValue = {
  user: AuthUser | null;
  token: string | null;
  isAuthHydrated: boolean;
  isAuthenticated: boolean;
  isAuthenticating: boolean;
  authError: string | null;
  login: (input: { email: string; password: string }) => Promise<boolean>;
  signup: (input: { name: string; email: string; password: string }) => Promise<boolean>;
  logout: () => Promise<void>;
  clearAuthError: () => void;
};

type PersistedAuth = {
  token: string;
  user: AuthUser;
};

const AUTH_STORAGE_KEY = "@madlab/auth:v1";

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function toErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  return "Authentication failed.";
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isAuthHydrated, setIsAuthHydrated] = useState(false);
  const [isAuthenticating, setIsAuthenticating] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);

  const clearAuthError = () => {
    setAuthError(null);
  };

  const persistAuth = async (payload: PersistedAuth | null) => {
    if (!payload) {
      await AsyncStorage.removeItem(AUTH_STORAGE_KEY);
      return;
    }

    await AsyncStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(payload));
  };

  const applyAuth = async (payload: PersistedAuth | null) => {
    if (!payload) {
      setApiAuthToken(null);
      setToken(null);
      setUser(null);
      await persistAuth(null);
      return;
    }

    setApiAuthToken(payload.token);
    setToken(payload.token);
    setUser(payload.user);
    await persistAuth(payload);
  };

  useEffect(() => {
    let active = true;

    const hydrateAuth = async () => {
      try {
        const raw = await AsyncStorage.getItem(AUTH_STORAGE_KEY);
        if (!raw) {
          if (active) {
            setApiAuthToken(null);
            setIsAuthHydrated(true);
          }
          return;
        }

        const parsed = JSON.parse(raw) as Partial<PersistedAuth>;
        const persistedToken = String(parsed.token || "").trim();

        if (!persistedToken) {
          await AsyncStorage.removeItem(AUTH_STORAGE_KEY);
          if (active) {
            setApiAuthToken(null);
            setIsAuthHydrated(true);
          }
          return;
        }

        setApiAuthToken(persistedToken);

        try {
          const currentUser = await fetchCurrentUser();
          if (!active) {
            return;
          }

          setToken(persistedToken);
          setUser(currentUser);
          await persistAuth({ token: persistedToken, user: currentUser });
        } catch {
          await AsyncStorage.removeItem(AUTH_STORAGE_KEY);
          if (active) {
            setApiAuthToken(null);
            setToken(null);
            setUser(null);
          }
        }
      } catch {
        if (active) {
          setApiAuthToken(null);
          setToken(null);
          setUser(null);
        }
      } finally {
        if (active) {
          setIsAuthHydrated(true);
        }
      }
    };

    void hydrateAuth();

    return () => {
      active = false;
    };
  }, []);

  const login = async (input: { email: string; password: string }): Promise<boolean> => {
    setIsAuthenticating(true);
    setAuthError(null);

    try {
      const response = await loginRequest(input);
      await applyAuth({ token: response.token, user: response.user });
      return true;
    } catch (error) {
      setAuthError(toErrorMessage(error));
      return false;
    } finally {
      setIsAuthenticating(false);
    }
  };

  const signup = async (input: {
    name: string;
    email: string;
    password: string;
  }): Promise<boolean> => {
    setIsAuthenticating(true);
    setAuthError(null);

    try {
      const response = await signUpRequest(input);
      await applyAuth({ token: response.token, user: response.user });
      return true;
    } catch (error) {
      setAuthError(toErrorMessage(error));
      return false;
    } finally {
      setIsAuthenticating(false);
    }
  };

  const logout = async (): Promise<void> => {
    try {
      if (token) {
        await logoutRequest();
      }
    } catch {
      // logout should clear local session even if backend call fails
    } finally {
      await applyAuth(null);
      setAuthError(null);
    }
  };

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      token,
      isAuthHydrated,
      isAuthenticated: Boolean(user && token),
      isAuthenticating,
      authError,
      login,
      signup,
      logout,
      clearAuthError,
    }),
    [authError, isAuthenticating, isAuthHydrated, token, user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
