const LOCAL_STORAGE_KEY = 'selectedLanguages';

type AllType = { [projectId: number]: string[] | undefined };

export class ProjectPreferencesService {
  getAll(): AllType {
    return JSON.parse(localStorage.getItem(LOCAL_STORAGE_KEY) as string);
  }

  getForProject(projectId: number): string[] {
    if (!this.getAll()) {
      return [];
    }
    return Array.from(new Set(this.getAll()[projectId]));
  }

  setForProject(projectId, languages: string[] | undefined) {
    this.setAll({ ...this.getAll(), [projectId]: languages });
  }

  setAll(data: AllType) {
    return localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(data));
  }

  getFiltered(projectId, allLanguages: string[]): string[] {
    let filtered = [...this.getForProject(projectId)].filter((f) =>
      allLanguages.includes(f)
    );

    //filter the value, to get rid of potentially removed items
    if (filtered.length == 0 && allLanguages.length) {
      filtered = allLanguages.slice(0, 1);
    }

    return filtered;
  }
}

export const projectPreferencesService = new ProjectPreferencesService();
