import {container, singleton} from 'tsyringe';
import {ApiHttpService} from './apiHttpService';
import {LanguageDTO} from './response.types';

const http = container.resolve(ApiHttpService);

@singleton()
export class languageService {
    public getLanguages = async (repositoryId: number): Promise<LanguageDTO[]> =>
        (await http.fetch(`repository/${repositoryId}/languages`)).json();

    get = (repositoryId, languageId): Promise<LanguageDTO> => http.get(`repository/${repositoryId}/languages/${languageId}`);

    create = (repositoryId, value: LanguageDTO): Promise<LanguageDTO> => http.post(`repository/${repositoryId}/languages`, value);

    async editLanguage(repositoryId: number, data: LanguageDTO): Promise<LanguageDTO> {
        return await (await http.postNoJson(`repository/${repositoryId}/languages/edit`, data)).json();
    }

    delete(repositoryId: number, id: number): Promise<null> {
        return http.delete("repository/" + repositoryId + "/languages/" + id);
    }
}
