import { container, singleton } from 'tsyringe';
import { ApiV1HttpService } from './http/ApiV1HttpService';
import { LanguageDTO } from './response.types';

const http = container.resolve(ApiV1HttpService);

@singleton()
export class LanguageService {
  public getLanguages = async (projectId: number): Promise<LanguageDTO[]> =>
    (await http.fetch(`project/${projectId}/languages`)).json();

  get = (projectId, languageId): Promise<LanguageDTO> =>
    http.get(`project/${projectId}/languages/${languageId}`);

  create = (projectId, value: LanguageDTO): Promise<LanguageDTO> =>
    http.post(`project/${projectId}/languages`, value);

  async editLanguage(
    projectId: number,
    data: LanguageDTO
  ): Promise<LanguageDTO> {
    return await (
      await http.postNoJson(`project/${projectId}/languages/edit`, data)
    ).json();
  }

  delete(projectId: number, id: number): Promise<null> {
    return http.delete('project/' + projectId + '/languages/' + id);
  }
}
