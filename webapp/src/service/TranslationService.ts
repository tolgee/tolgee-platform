import { singleton } from 'tsyringe';
import { ApiV1HttpService } from './http/ApiV1HttpService';
import { MessageService } from './MessageService';
import { TranslationsDataResponse, TranslationsObject } from './response.types';
import { TranslationCreationValue } from '../component/Translations/TranslationCreationDialog';
import { RepositoryPreferencesService } from './RepositoryPreferencesService';

@singleton()
export class TranslationService {
  constructor(
    private http: ApiV1HttpService,
    private messaging: MessageService,
    private selectedLanguagesService: RepositoryPreferencesService
  ) {}

  public getTranslations = async (
    repositoryId: number,
    langs?: string[],
    search?: string,
    limit?: number,
    offset?: number
  ): Promise<TranslationsDataResponse> => {
    const result: TranslationsDataResponse = await this.http.get(
      `repository/${repositoryId}/translations/view`,
      { search, languages: langs ? langs.join(',') : null, limit, offset }
    );

    this.selectedLanguagesService.setForRepository(
      repositoryId,
      new Set(result.params.languages)
    );

    return result;
  };

  createKey = (repositoryId: number, value: TranslationCreationValue) =>
    this.http.post(`repository/${repositoryId}/keys/create`, {
      key: value.key,
      translations: value.translations,
    });

  set = (
    repositoryId: number,
    dto: { key: string; translations: { [abbreviation: string]: string } }
  ) => this.http.post(`repository/${repositoryId}/translations`, dto);

  editKey = (
    repositoryId: number,
    dto: { oldFullPathString: string; newFullPathString: string }
  ) => this.http.post(`repository/${repositoryId}/keys/edit`, dto);

  setTranslations = (
    repositoryId: number,
    dto: { key: string; translations: TranslationsObject }
  ) => this.http.post(`repository/${repositoryId}/translations/set`, dto);

  deleteKey = (repositoryId: number, ids: number[]) =>
    this.http.delete(`repository/${repositoryId}/keys`, ids);
}
