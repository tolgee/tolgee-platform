import { useApiMutation } from 'tg.service/http/useQueryApi';

export const usePutKey = () =>
  useApiMutation({
    url: '/v2/projects/{projectId}/keys/{id}',
    method: 'put',
  });

export const usePostKey = () =>
  useApiMutation({
    url: '/v2/projects/{projectId}/keys/create',
    method: 'post',
  });

export const useDeleteKeys = () =>
  useApiMutation({
    url: '/v2/projects/{projectId}/keys',
    method: 'delete',
  });

export const usePutTranslation = () =>
  useApiMutation({
    url: '/v2/projects/{projectId}/translations',
    method: 'put',
  });

export const usePutTranslationState = () =>
  useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/set-state/{state}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/translations/{translationId}',
  });

export const usePutTag = () =>
  useApiMutation({
    url: '/v2/projects/{projectId}/keys/{keyId}/tags',
    method: 'put',
  });

export const useDeleteTag = () =>
  useApiMutation({
    url: '/v2/projects/{projectId}/keys/{keyId}/tags/{tagId}',
    method: 'delete',
  });

export const usePutTask = () =>
  useApiMutation({
    url: '/v2/projects/{projectId}/tasks/{taskId}',
    method: 'put',
  });

export const usePutTaskTranslation = () =>
  useApiMutation({
    url: '/v2/projects/{projectId}/tasks/{taskId}/keys/{keyId}',
    method: 'put',
  });
