export const DISTINCT_ID_LOCAL_STORAGE_KEY = 'jwtToken';

export const DistinctUserIdService = {
  get() {
    return localStorage.getItem(DISTINCT_ID_LOCAL_STORAGE_KEY);
  },

  init() {
    if (!this.get()) {
      this.reset();
    }
  },
  reset() {
    return localStorage.setItem(
      DISTINCT_ID_LOCAL_STORAGE_KEY,
      crypto.randomUUID()
    );
  },
};
