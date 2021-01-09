import {singleton} from 'tsyringe';

const LOCAL_STORAGE_KEY = 'selectedLanguages';

type AllType = { [repositoryId: number]: string[] };

@singleton()
export class repositoryPreferencesService {
    getAll(): AllType {
        return JSON.parse(localStorage.getItem(LOCAL_STORAGE_KEY));
    }

    getForRepository(repositoryId: number): Set<string> {
        if (!this.getAll()) {
            return new Set();
        }
        return new Set(this.getAll()[repositoryId]);
    }

    setForRepository(repositoryId, languages: Set<string>) {
        this.setAll({...this.getAll(), [repositoryId]: Array.from(languages)});
    }

    setAll(data: AllType) {
        return localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(data));
    }

    getUpdated(repositoryId, allLanguages: Set<string>): Set<string> {
        let filtered = [...this.getForRepository(repositoryId)].filter(f => allLanguages.has(f));

        //filter the value, to get rid of potentially removed items
        if (filtered.length == 0 && allLanguages.size) {
            filtered = Array.from(allLanguages).slice(0, 1);
        }

        const filteredSet = new Set(filtered);

        //update id on db change
        this.setForRepository(repositoryId, filteredSet);

        return filteredSet;
    }
}
