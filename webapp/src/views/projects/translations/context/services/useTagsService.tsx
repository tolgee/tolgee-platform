import { useQueryClient } from 'react-query';

import { useProject } from 'tg.hooks/useProject';
import { invalidateUrlPrefix } from 'tg.service/http/useQueryApi';
import { useDeleteTag, usePutTag } from 'tg.service/TranslationHooks';
import { AddTag, RemoveTag } from '../types';
import { useTranslationsService } from './useTranslationsService';

type Props = {
  translations: ReturnType<typeof useTranslationsService>;
};

export const useTagsService = ({ translations }: Props) => {
  const queryClient = useQueryClient();
  const putTag = usePutTag();
  const deleteTag = useDeleteTag();
  const project = useProject();

  const removeTag = (data: RemoveTag) =>
    deleteTag
      .mutateAsync({
        path: {
          keyId: data.keyId,
          tagId: data.tagId,
          projectId: project.id,
        },
      })
      .then(() => {
        const previousTags = translations.fixedTranslations?.find(
          (key) => key.keyId === data.keyId
        )?.keyTags;
        invalidateUrlPrefix(queryClient, '/v2/projects/{projectId}/tags');
        translations.updateTranslationKeys([
          {
            keyId: data.keyId,
            value: {
              keyTags: previousTags?.filter((t) => t.id !== data.tagId),
            },
          },
        ]);
      });

  const addTag = (data: AddTag) =>
    putTag
      .mutateAsync({
        path: { projectId: project.id, keyId: data.keyId },
        content: { 'application/json': { name: data.name } },
      })
      .then((response) => {
        const previousTags =
          translations.fixedTranslations
            ?.find((key) => key.keyId === data.keyId)
            ?.keyTags.filter((t) => t.id !== response.id) || [];
        translations.updateTranslationKeys([
          {
            keyId: data.keyId,
            value: {
              keyTags: [...previousTags, response!],
            },
          },
        ]);
        invalidateUrlPrefix(queryClient, '/v2/projects/{projectId}/tags');
        data.onSuccess?.();
      })
      .catch((e) => {
        // return never fullfilling promise to prevent after action
        return new Promise(() => {});
      });

  return {
    removeTag,
    addTag,
    isLoading: deleteTag.isLoading || putTag.isLoading,
  };
};
