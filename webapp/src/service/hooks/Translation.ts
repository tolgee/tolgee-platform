import { useMutation, useQuery, UseQueryOptions } from 'react-query';
import { container } from 'tsyringe';

import { ApiV1HttpService } from '../http/ApiV1HttpService';
import { components } from '../apiSchema.generated';

const httpV1 = container.resolve(ApiV1HttpService);

export type TranslationsType =
  components['schemas']['ViewDataResponseLinkedHashSetKeyWithTranslationsResponseDtoResponseParams'];
export type SetTranslationsBody = components['schemas']['SetTranslationsDTO'];
export type CreateKeyBody = components['schemas']['SetTranslationsDTO'];
export type EditKeyBody = components['schemas']['DeprecatedEditKeyDTO'];

export const useGetTranslations = (
  projectId: number,
  langs?: string[],
  search?: string,
  limit?: number,
  offset?: number,
  options?: UseQueryOptions<TranslationsType>
) => {
  const params = {
    search,
    languages: langs ? langs.join(',') : null,
    limit,
    offset,
  };

  return useQuery<TranslationsType>(
    ['project', projectId, 'translations', 'view', params],
    () => httpV1.get(`project/${projectId}/translations/view`, params),
    options
  );
};

export const useSetTranslations = (projectId: number) =>
  useMutation(
    ['project', projectId, 'translations'],
    (value: SetTranslationsBody) =>
      httpV1.put(`project/${projectId}/translations`, value)
  );

export const useCreateKey = (projectId: number) =>
  useMutation(
    ['project', projectId, 'keys', 'create'],
    (value: CreateKeyBody) =>
      httpV1.post(`project/${projectId}/keys/create`, {
        key: value.key,
        translations: value.translations,
      })
  );

export const useEditKey = (projectId: number) =>
  useMutation(['project', projectId, 'keys', 'edit'], (value: EditKeyBody) =>
    httpV1.post(`project/${projectId}/keys/edit`, value)
  );

export const useDeleteKey = (projectId: number) =>
  useMutation(['project', projectId, 'keys'], (ids: number[]) =>
    httpV1.delete(`project/${projectId}/keys`, ids)
  );
