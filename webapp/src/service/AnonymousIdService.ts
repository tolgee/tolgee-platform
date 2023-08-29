import { v4 as uuidv4 } from 'uuid';

export const ANONYMOUS_ID_LOCAL_STORAGE_KEY = 'anonymousUserId';

export const AnonymousIdService = {
  get() {
    return localStorage.getItem(ANONYMOUS_ID_LOCAL_STORAGE_KEY);
  },
  init() {
    if (!this.get()) {
      this.reset();
    }
  },
  reset() {
    return localStorage.setItem(ANONYMOUS_ID_LOCAL_STORAGE_KEY, uuidv4());
  },
  dispose() {
    return localStorage.removeItem(ANONYMOUS_ID_LOCAL_STORAGE_KEY);
  },
};
