import { useState } from 'react';
import { container } from 'tsyringe';
import { useTranslate } from '@tolgee/react';

import { confirmation } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { MessageService } from 'tg.service/MessageService';
import { useDeleteKeys } from 'tg.service/TranslationHooks';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { useTranslationsService } from './useTranslationsService';

type Props = {
  translations: ReturnType<typeof useTranslationsService>;
};

const messaging = container.resolve(MessageService);

export const useSelectionService = ({ translations }: Props) => {
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

  const clear = () => {
    setSelection([]);
  };

  const deleteSelected = () => {
    return new Promise<void>((resolve, reject) =>
      confirmation({
        title: t('translations_delete_selected'),
        message: t({
          key: 'translations_key_delete_confirmation_text',
          parameters: { count: String(selection.length) },
        }),
        onConfirm() {
          deleteKeys.mutate(
            {
              path: { projectId: project.id, ids: selection },
            },
            {
              onSuccess() {
                translations.refetchTranslations();
                messaging.success(
                  t('Translation grid - Successfully deleted!')
                );
                resolve();
              },
              onError(e) {
                const parsed = parseErrorResponse(e);
                parsed.forEach((error) => messaging.error(t(error)));
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
    isLoading: deleteKeys.isLoading,
    data: selection,
  };
};
