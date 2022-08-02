const LOCAL_STORAGE_KEY = 'invitationCode';

export class InvitationCodeService {
  getCode() {
    return localStorage.getItem(LOCAL_STORAGE_KEY);
  }

  disposeCode() {
    return localStorage.removeItem(LOCAL_STORAGE_KEY);
  }

  setCode(token: string) {
    return localStorage.setItem(LOCAL_STORAGE_KEY, token);
  }
}

export const invitationCodeService = new InvitationCodeService();
