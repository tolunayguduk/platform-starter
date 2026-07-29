import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import {
  fetchMe,
  login as loginApi,
  logout as logoutApi,
  refresh as refreshApi,
  type CurrentUser,
  type TokenResponse,
} from '../api/auth';

const REFRESH_TOKEN_KEY = 'platform.refreshToken';

interface AuthContextValue {
  accessToken: string | null;
  user: CurrentUser | null;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState<string | null>(null);
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Every authenticated page needs "who is this user" (navbar username, dashboard greeting) -
  // fetched once here, centrally, rather than every consumer calling /api/me on its own.
  useEffect(() => {
    if (!accessToken) {
      setUser(null);
      return;
    }
    let cancelled = false;
    fetchMe(accessToken).then((me) => {
      if (!cancelled) setUser(me);
    });
    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  // On first load, try to turn a stored refresh token into a fresh access token so a page
  // reload doesn't force the user back through /login.
  //
  // Keycloak's refresh tokens are single-use here (revokeRefreshToken + refreshTokenMaxReuse:0 -
  // see realm-platform.json), and React 18 StrictMode runs this effect twice in development.
  // The ref guard makes sure the real network call only ever fires once - deliberately with NO
  // "cancelled" cleanup flag guarding the .then/.finally callbacks. That flag used to be set by
  // this same effect's own cleanup (fired by StrictMode's first pass) before the request even
  // resolved, which discarded a perfectly successful token rotation and left isLoading stuck at
  // true forever (a permanently blank page). Since the ref already guarantees a single attempt,
  // there is nothing left for a cancellation flag to protect against here.
  const refreshAttempted = useRef(false);
  useEffect(() => {
    if (refreshAttempted.current) return;
    refreshAttempted.current = true;

    const stored = localStorage.getItem(REFRESH_TOKEN_KEY);
    if (!stored) {
      setIsLoading(false);
      return;
    }
    refreshApi(stored)
      .then((tokens) => applyTokens(tokens))
      .catch(() => {
        localStorage.removeItem(REFRESH_TOKEN_KEY);
      })
      .finally(() => setIsLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function applyTokens(tokens: TokenResponse) {
    setAccessToken(tokens.access_token);
    setRefreshToken(tokens.refresh_token);
    localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refresh_token);
  }

  async function login(username: string, password: string) {
    applyTokens(await loginApi(username, password));
  }

  async function logout() {
    const current = refreshToken;
    setAccessToken(null);
    setRefreshToken(null);
    setUser(null);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    if (current) {
      // Best-effort: local session is already cleared above regardless of Keycloak reachability.
      await logoutApi(current).catch(() => undefined);
    }
  }

  return (
    <AuthContext.Provider value={{ accessToken, user, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
