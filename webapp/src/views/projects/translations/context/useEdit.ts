import { useState } from 'react';

import { components } from 'tg.service/apiSchema.generated';
import { StateType } from 'tg.constants/translationStates';
import {
  usePutKey,
  usePutTranslation,
  usePutTranslationState,
  usePutTag,
  useDeleteTag,
} from 'tg.service/TranslationHooks';

export type Direction = 'DOWN';

export type CellLocation = {
  keyId: number;
  language?: string | undefined;
};

export type SetEditType = CellLocation & {
  value: string;
};

export type EditType = CellLocation & {
  value?: string;
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

  const putKey = usePutKey();
  const putTranslation = usePutTranslation();
  const putTranslationState = usePutTranslationState();
  const putTag = usePutTag();
  const deleteTag = useDeleteTag();

  const getTranslationKeyName = (keyId: number) =>
    translations!.find((t) => t.keyId === keyId)!.keyName;

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
      await putKey.mutateAsync({
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
    const { language, value, keyId } = payload;
    const keyName = getTranslationKeyName(keyId);

    const newVal =
      payload.value !== getEditOldValue()
        ? await putTranslation.mutateAsync({
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

  const mutateTranslationState = (translationId: number, state: StateType) =>
    putTranslationState.mutateAsync({
      path: {
        projectId,
        translationId,
        state,
      },
    });

  return {
    position,
    setPosition,
    isLoading:
      putKey.isLoading ||
      putTranslation.isLoading ||
      putTag.isLoading ||
      deleteTag.isLoading ||
      putTranslationState.isLoading,
    moveEditToDirection,
    mutateTranslation,
    mutateTranslationKey,
    mutateTranslationState,
  };
};
