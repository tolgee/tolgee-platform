export const INVITATION_CODE_STORAGE_KEY = 'invitationCode';

export const InvitationCodeService = {
  getCode() {
    return localStorage.getItem(INVITATION_CODE_STORAGE_KEY) || undefined;
  },

  disposeCode() {
    return localStorage.removeItem(INVITATION_CODE_STORAGE_KEY);
  },

  setCode(token: string) {
    return localStorage.setItem(INVITATION_CODE_STORAGE_KEY, token);
  },
};
