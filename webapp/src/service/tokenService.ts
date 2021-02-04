import {singleton} from 'tsyringe';

const LOCAL_STORAGE_KEY = 'jwtToken';

@singleton()
export class tokenService {
    getToken() {
        return localStorage.getItem(LOCAL_STORAGE_KEY);
    }

    disposeToken() {
        return localStorage.removeItem(LOCAL_STORAGE_KEY);
    }

    setToken(token: string) {
        return localStorage.setItem(LOCAL_STORAGE_KEY, token);
    }

    getParsed() {
        return this.parseJwt(this.getToken());
    }

    private parseJwt(token) {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));

        return JSON.parse(jsonPayload);
    };
}
