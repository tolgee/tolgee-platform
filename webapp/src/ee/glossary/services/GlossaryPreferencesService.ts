const LOCAL_STORAGE_KEY = '__tolgee_glossarySelectedLanguages';

type AllType = { [glossaryId: number]: string[] | undefined };

export class GlossaryPreferencesService {
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

  getForGlossary(glossaryId: number): string[] | undefined {
    return this.getAll()[glossaryId];
  }

  setForGlossary(glossaryId: number, languages: string[]) {
    this.setAll({ ...this.getAll(), [glossaryId]: languages });
  }
}

export const glossaryPreferencesService = new GlossaryPreferencesService();
