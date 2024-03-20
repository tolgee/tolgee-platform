import { AnonymousIdService } from './AnonymousIdService';

export const JWT_LOCAL_STORAGE_KEY = 'jwtToken';
export const ADMIN_JWT_LOCAL_STORAGE_KEY = 'adminJwtToken';

export class TokenService {
  getToken() {
    const token = localStorage.getItem(JWT_LOCAL_STORAGE_KEY) || undefined;
    if (!token) {
      AnonymousIdService.init();
    }
    return token;
  }

  disposeAllTokens() {
    this.disposeToken();
    this.disposeAdminToken();
  }

  disposeToken() {
    AnonymousIdService.reset();
    return localStorage.removeItem(JWT_LOCAL_STORAGE_KEY);
  }

  setToken(token: string) {
    return localStorage.setItem(JWT_LOCAL_STORAGE_KEY, token);
  }

  getAdminToken() {
    return localStorage.getItem(ADMIN_JWT_LOCAL_STORAGE_KEY);
  }

  disposeAdminToken() {
    return localStorage.removeItem(ADMIN_JWT_LOCAL_STORAGE_KEY);
  }

  setAdminToken(token: string) {
    return localStorage.setItem(ADMIN_JWT_LOCAL_STORAGE_KEY, token);
  }
}

export const tokenService = new TokenService();
