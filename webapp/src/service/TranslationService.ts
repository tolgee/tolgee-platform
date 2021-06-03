import { singleton } from 'tsyringe';
import { ApiV1HttpService } from './http/ApiV1HttpService';
import { MessageService } from './MessageService';
import { TranslationsDataResponse, TranslationsObject } from './response.types';
import { TranslationCreationValue } from '../component/Translations/TranslationCreationDialog';
import { ProjectPreferencesService } from './ProjectPreferencesService';

@singleton()
export class TranslationService {
  constructor(
    private http: ApiV1HttpService,
    private messaging: MessageService,
    private selectedLanguagesService: ProjectPreferencesService
  ) {}

  public getTranslations = async (
    projectId: number,
    langs?: string[],
    search?: string,
    limit?: number,
    offset?: number
  ): Promise<TranslationsDataResponse> => {
    const result: TranslationsDataResponse = await this.http.get(
      `project/${projectId}/translations/view`,
      { search, languages: langs ? langs.join(',') : null, limit, offset }
    );

    this.selectedLanguagesService.setForProject(
      projectId,
      new Set(result.params.languages)
    );

    return result;
  };

  createKey = (projectId: number, value: TranslationCreationValue) =>
    this.http.post(`project/${projectId}/keys/create`, {
      key: value.key,
      translations: value.translations,
    });

  set = (
    projectId: number,
    dto: { key: string; translations: { [abbreviation: string]: string } }
  ) => this.http.post(`project/${projectId}/translations`, dto);

  editKey = (
    projectId: number,
    dto: { oldFullPathString: string; newFullPathString: string }
  ) => this.http.post(`project/${projectId}/keys/edit`, dto);

  setTranslations = (
    projectId: number,
    dto: { key: string; translations: TranslationsObject }
  ) => this.http.post(`project/${projectId}/translations/set`, dto);

  deleteKey = (projectId: number, ids: number[]) =>
    this.http.delete(`project/${projectId}/keys`, ids);
}
