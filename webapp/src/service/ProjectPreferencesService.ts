import { singleton } from 'tsyringe';

const LOCAL_STORAGE_KEY = 'selectedLanguages';

type AllType = { [projectId: number]: string[] };

@singleton()
export class ProjectPreferencesService {
  getAll(): AllType {
    return JSON.parse(localStorage.getItem(LOCAL_STORAGE_KEY) as string);
  }

  getForProject(projectId: number): Set<string> {
    if (!this.getAll()) {
      return new Set();
    }
    return new Set(this.getAll()[projectId]);
  }

  setForProject(projectId, languages: Set<string>) {
    this.setAll({ ...this.getAll(), [projectId]: Array.from(languages) });
  }

  setAll(data: AllType) {
    return localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(data));
  }

  getUpdated(projectId, allLanguages: Set<string>): Set<string> {
    let filtered = [...this.getForProject(projectId)].filter((f) =>
      allLanguages.has(f)
    );

    //filter the value, to get rid of potentially removed items
    if (filtered.length == 0 && allLanguages.size) {
      filtered = Array.from(allLanguages).slice(0, 1);
    }

    const filteredSet = new Set(filtered);

    //update id on db change
    this.setForProject(projectId, filteredSet);

    return filteredSet;
  }
}
