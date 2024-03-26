import { AnonymousIdService } from './AnonymousIdService';

export const JWT_LOCAL_STORAGE_KEY = 'jwtToken';
export const ADMIN_JWT_LOCAL_STORAGE_KEY = 'adminJwtToken';

type JwtTokenType = {
  aud: string;
  exp: number;
  iat: number;
  ste: number;
  sub: string;
};

export const tokenService = {
  getToken() {
    const token = localStorage.getItem(JWT_LOCAL_STORAGE_KEY) || undefined;
    if (!token) {
      AnonymousIdService.init();
    }
    return token;
  },

  disposeAllTokens() {
    tokenService.disposeToken();
    tokenService.disposeAdminToken();
  },

  disposeToken() {
    AnonymousIdService.reset();
    return localStorage.removeItem(JWT_LOCAL_STORAGE_KEY);
  },

  setToken(token: string) {
    return localStorage.setItem(JWT_LOCAL_STORAGE_KEY, token);
  },

  getAdminToken() {
    return localStorage.getItem(ADMIN_JWT_LOCAL_STORAGE_KEY);
  },

  disposeAdminToken() {
    return localStorage.removeItem(ADMIN_JWT_LOCAL_STORAGE_KEY);
  },

  setAdminToken(token: string) {
    return localStorage.setItem(ADMIN_JWT_LOCAL_STORAGE_KEY, token);
  },

  parseJwt(token: string) {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(function (c) {
          return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        })
        .join('')
    );

    return JSON.parse(jsonPayload) as JwtTokenType;
  },

  getUserId(token: string | undefined) {
    if (!token) {
      return undefined;
    }
    try {
      const parsed = tokenService.parseJwt(token);
      const result = Number(parsed.sub);
      return Number.isNaN(result) ? undefined : result;
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error(e);
      return undefined;
    }
  },
};
