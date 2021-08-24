import { useState } from 'react';

import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';

export type Direction = 'DOWN';

type CellLocation = {
  keyId: number;
  keyName: string;
  language?: string;
};

export type SetEditType = CellLocation & {
  value: string;
};

export type EditType = CellLocation & {
  savedValue?: string;
  changed?: boolean;
  mode: EditModeType;
};

export type EditModeType = 'editor' | 'comments';

type KeyWithTranslationsModelType =
  components['schemas']['KeyWithTranslationsModel'];

type Props = {
  projectId: number;
  translations: KeyWithTranslationsModelType[] | undefined;
};

export const useEdit = ({ projectId, translations }: Props) => {
  const [position, setPosition] = useState<EditType | undefined>(undefined);

  const updateValue = useApiMutation({
    url: '/v2/projects/{projectId}/translations',
    method: 'put',
  });

  const updateKey = useApiMutation({
    url: '/v2/projects/{projectId}/keys/{id}',
    method: 'put',
  });

  const moveEditToDirection = (direction: Direction | undefined) => {
    const currentIndex =
      translations?.findIndex((k) => k.keyId === position?.keyId) || 0;
    if (currentIndex === -1 || !direction) {
      setPosition(undefined);
      return;
    }
    let nextKey = undefined as KeyWithTranslationsModelType | undefined;
    if (direction === 'DOWN') {
      nextKey = translations?.[currentIndex + 1];
    } else if (direction === 'UP') {
      nextKey = translations?.[currentIndex - 1];
    }
    setPosition(
      nextKey
        ? {
            keyId: nextKey.keyId,
            keyName: nextKey.keyName,
            language: position?.language,
            mode: 'editor',
          }
        : undefined
    );
  };

  const getEditOldValue = (): string | undefined => {
    const key = translations?.find((k) => k.keyId === position?.keyId);
    if (key) {
      return (
        (position?.language
          ? key.translations[position.language]?.text
          : key.keyName) || ''
      );
    }
  };

  const mutateTranslationKey = async (payload: SetEditType) => {
    if (payload.value !== getEditOldValue()) {
      await updateKey.mutateAsync({
        path: { projectId, id: payload.keyId },
        content: {
          'application/json': {
            name: payload.value,
          },
        },
      });
    }
  };

  const mutateTranslation = async (payload: SetEditType) => {
    const { keyName, language, value } = payload;

    const newVal =
      payload.value !== getEditOldValue()
        ? await updateValue.mutateAsync({
            path: { projectId },
            content: {
              'application/json': {
                key: keyName,
                translations: {
                  [language!]: value,
                },
              },
            },
          })
        : null;
    return newVal;
  };

  return {
    position,
    setPosition,
    isLoading: updateKey.isLoading || updateValue.isLoading,
    moveEditToDirection,
    mutateTranslation,
    mutateTranslationKey,
  };
};
