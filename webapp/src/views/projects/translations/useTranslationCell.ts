import React, { useEffect, useMemo } from 'react';
import { getTolgeeFormat } from '@tginternal/editor';

import { TRANSLATION_STATES } from 'tg.constants/translationStates';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { components } from 'tg.service/apiSchema.generated';

import {
  useTranslationsActions,
  useTranslationsSelector,
} from './context/TranslationsContext';
import {
  AfterCommand,
  DeletableKeyWithTranslationsModelType,
  EditMode,
} from './context/types';
import { useProject } from 'tg.hooks/useProject';

type LanguageModel = components['schemas']['LanguageModel'];

export type SaveProps = {
  preventTaskResolution?: boolean;
  after?: AfterCommand;
};

type Props = {
  keyData: DeletableKeyWithTranslationsModelType;
  language: LanguageModel;
  onSaveSuccess?: (val: string) => void;
  cellRef: React.RefObject<HTMLElement>;
};

export const useTranslationCell = ({
  keyData,
  language,
  onSaveSuccess,
  cellRef,
}: Props) => {
  const project = useProject();
  const {
    setEditValue,
    setEditValueString,
    registerElement,
    unregisterElement,
    setEdit,
    changeField,
    setEditForce,
    setTranslationState,
    setTaskState,
    updateEdit,
  } = useTranslationsActions();

  const { satisfiesLanguageAccess } = useProjectPermissions();

  const keyId = keyData.keyId;
  const langTag = language.tag;

  const cursor = useTranslationsSelector((v) => {
    return v.cursor?.keyId === keyId ? v.cursor : undefined;
  });

  const baseLanguage = useTranslationsSelector((c) =>
    c.languages?.find((l) => l.base)
  );

  const isEditingRow = Boolean(cursor?.keyId === keyId);
  const isEditing = Boolean(isEditingRow && cursor?.language === langTag);

  const value =
    (isEditing && cursor?.value.variants[cursor.activeVariant ?? 'other']) ||
    '';

  useEffect(() => {
    registerElement({ keyId, language: langTag, ref: cellRef.current! });
    return () => {
      unregisterElement({ keyId, language: langTag, ref: cellRef.current! });
    };
  }, [cellRef.current, keyId, langTag]);

  const handleOpen = (mode?: EditMode) => {
    setEdit({
      keyId,
      language: langTag,
      mode,
    });
  };

  const handleSave = ({ after, preventTaskResolution }: SaveProps) => {
    changeField({
      after,
      preventTaskResolution,
      onSuccess: () => onSaveSuccess?.(value),
    });
  };

  const getBaseText = () => {
    if (!baseLanguage) {
      return undefined;
    }

    return keyData.translations[baseLanguage.tag]?.text;
  };

  const baseText = getBaseText();

  const handleInsertBase = () => {
    const baseText = getBaseText();

    if (!baseText) {
      return;
    }

    let baseVariant: string | undefined;
    if (cursor?.activeVariant) {
      const variants = getTolgeeFormat(
        baseText || '',
        keyData.keyIsPlural,
        !project.icuPlaceholders
      )?.variants;
      baseVariant = variants?.[cursor.activeVariant] ?? variants?.['other'];
    } else {
      baseVariant = baseText;
    }

    if (baseVariant) {
      setEditValueString(baseVariant);
    }
  };

  const baseValue = useMemo(() => {
    return getTolgeeFormat(
      getBaseText() || '',
      keyData.keyIsPlural,
      !project.icuPlaceholders
    );
  }, [baseText, keyData.keyIsPlural]);

  const handleClose = (force = false) => {
    if (force) {
      setEditForce(undefined);
    } else {
      setEdit(undefined);
    }
  };

  const translation = langTag ? keyData?.translations[langTag] : undefined;

  const firstTask = keyData.tasks?.find((t) => t.languageTag === language.tag);
  const assignedTask = keyData.tasks?.find(
    (t) => t.languageTag === language.tag && t.userAssigned
  );

  const setAssignedTaskState = (done: boolean) => {
    if (firstTask) {
      setTaskState({
        keyId: keyData.keyId,
        taskNumber: firstTask.number,
        done,
      });
    }
  };

  const setState = () => {
    if (!translation) {
      return;
    }
    const nextState = TRANSLATION_STATES[translation.state]?.next;
    if (nextState) {
      setTranslationState({
        state: nextState,
        keyId,
        translationId: translation!.id,
        language: langTag!,
      });
    }
  };

  function setVariant(activeVariant: string | undefined) {
    updateEdit({ activeVariant });
  }

  const canChangeState =
    (assignedTask?.userAssigned && assignedTask.type === 'REVIEW') ||
    satisfiesLanguageAccess('translations.state-edit', language.id);

  const disabled = translation?.state === 'DISABLED';
  const editEnabled =
    ((assignedTask?.userAssigned && assignedTask.type === 'TRANSLATE') ||
      satisfiesLanguageAccess('translations.edit', language.id)) &&
    !disabled;

  return {
    keyId,
    language,
    handleOpen,
    handleClose,
    handleSave,
    handleInsertBase,
    setEditValue,
    setEditValueString,
    setState,
    setVariant,
    setAssignedTaskState,
    value,
    editVal: isEditing ? cursor : undefined,
    isEditing,
    isEditingRow,
    editingLanguageTag: cursor?.language,
    autofocus: true,
    keyData,
    canChangeState,
    editEnabled,
    translation,
    disabled,
    baseValue,
    baseText,
  };
};
