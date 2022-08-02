import { useState } from 'react';
import { useTranslate, T } from '@tolgee/react';

import { confirmation } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { messageService } from 'tg.service/MessageService';
import { useDeleteKeys } from 'tg.service/TranslationHooks';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { useTranslationsService } from './useTranslationsService';
import { useOrganizationUsageMethods } from 'tg.globalContext/helpers';

type Props = {
  translations: ReturnType<typeof useTranslationsService>;
};

export const useSelectionService = ({ translations }: Props) => {
  const { refetchUsage } = useOrganizationUsageMethods();
  const [selection, setSelection] = useState<number[]>([]);
  const t = useTranslate();
  const deleteKeys = useDeleteKeys();
  const project = useProject();

  const toggle = (keyId: number) => {
    const newSelection = selection.includes(keyId)
      ? selection.filter((s) => s !== keyId)
      : [...selection, keyId];
    setSelection(newSelection);
  };

  const select = (data: number[]) => {
    setSelection(data);
  };

  const clear = () => {
    setSelection([]);
  };

  const deleteSelected = () => {
    return new Promise<void>((resolve, reject) =>
      confirmation({
        title: t('translations_delete_selected'),
        message: t('translations_key_delete_confirmation_text', {
          count: String(selection.length),
        }),
        onConfirm() {
          deleteKeys.mutate(
            {
              path: { projectId: project.id },
              content: { 'application/json': { ids: selection } },
            },
            {
              onSuccess() {
                translations.refetchTranslations();
                refetchUsage();
                messageService.success(
                  t('Translation grid - Successfully deleted!')
                );
                resolve();
              },
              onError(e) {
                const parsed = parseErrorResponse(e);
                parsed.forEach((error) => messageService.error(<T>{error}</T>));
                reject(e);
              },
            }
          );
        },
      })
    );
  };

  return {
    toggle,
    clear,
    deleteSelected,
    select,
    isDeleting: deleteKeys.isLoading,
    data: selection,
  };
};
