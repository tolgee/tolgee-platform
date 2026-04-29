const LOCAL_STORAGE_KEY = '__tolgee_tmSelectedLanguages';

type AllType = { [tmId: number]: string[] | undefined };

class TmPreferencesService {
  private getAll(): AllType {
    try {
      return JSON.parse(localStorage.getItem(LOCAL_STORAGE_KEY) || '{}');
    } catch {
      return {};
    }
  }

  private setAll(data: AllType) {
    try {
      localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(data));
    } catch {
      // ignore
    }
  }

  getForTm(tmId: number): string[] | undefined {
    return this.getAll()[tmId];
  }

  setForTm(tmId: number, languages: string[]) {
    this.setAll({ ...this.getAll(), [tmId]: languages });
  }
}

export const tmPreferencesService = new TmPreferencesService();
