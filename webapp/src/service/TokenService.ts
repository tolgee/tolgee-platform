import { singleton } from 'tsyringe';

export const JWT_LOCAL_STORAGE_KEY = 'jwtToken';
export const ADMIN_JWT_LOCAL_STORAGE_KEY = 'adminJwtToken';

@singleton()
export class TokenService {
  getToken() {
    return localStorage.getItem(JWT_LOCAL_STORAGE_KEY);
  }

  disposeToken() {
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

  getParsed() {
    return this.parseJwt(this.getToken());
  }

  private parseJwt(token) {
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

    return JSON.parse(jsonPayload);
  }
}
