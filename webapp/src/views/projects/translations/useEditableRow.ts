import React, { useEffect, useRef } from 'react';

import {
  useTranslationsSelector,
  useTranslationsActions,
} from './context/TranslationsContext';
import { AfterCommand, EditMode } from './context/types';

type Props = {
  keyId: number;
  keyName: string;
  language: string | undefined;
  defaultVal?: string;
  onSaveSuccess?: (val: string) => void;
  cellRef: React.RefObject<HTMLElement>;
};

export const useEditableRow = ({
  keyId,
  defaultVal,
  language,
  onSaveSuccess,
  cellRef,
}: Props) => {
  const {
    updateEdit,
    registerElement,
    unregisterElement,
    setEdit,
    changeField,
    setEditForce,
  } = useTranslationsActions();

  const cursor = useTranslationsSelector((v) => {
    return v.cursor?.keyId === keyId ? v.cursor : undefined;
  });

  const isEditingRow = Boolean(cursor?.keyId === keyId);
  const isEditing = Boolean(isEditingRow && cursor?.language === language);

  const value = (isEditing && cursor?.value) || '';

  const originalValue = (isEditing && defaultVal) || '';

  const setValue = (val: string) => updateEdit({ value: val });

  useEffect(() => {
    registerElement({ keyId, language, ref: cellRef.current! });
    return () => {
      unregisterElement({ keyId, language, ref: cellRef.current! });
    };
  }, [cellRef.current, keyId, language]);

  useEffect(() => {
    if (isEditing) {
      setValue(cursor?.value || originalValue);
    }
  }, [isEditing, originalValue]);

  const handleOpen = (mode: EditMode) => {
    setEdit({
      keyId,
      language,
      mode,
    });
  };

  const handleSave = (after?: AfterCommand) => {
    changeField({
      after,
      onSuccess: () => onSaveSuccess?.(value),
    });
  };

  const handleClose = (force = false) => {
    if (force) {
      setEditForce(undefined);
    } else {
      setEdit(undefined);
    }
  };

  const handleModeChange = (mode: EditMode) => {
    updateEdit({ mode });
  };

  useEffect(() => {
    const isChanged = originalValue !== value;
    // let context know, that something has changed
    if (isEditing && Boolean(cursor?.changed) !== isChanged) {
      updateEdit({ changed: isChanged });
    }
  }, [originalValue, cursor?.changed, value]);

  const valueRef = useRef(isEditing ? value : undefined);

  useEffect(() => {
    valueRef.current = isEditing ? value : undefined;
  }, [value, isEditing]);

  return {
    handleOpen,
    handleClose,
    handleSave,
    handleModeChange,
    value,
    setValue,
    editVal: isEditing ? cursor : undefined,
    isEditing,
    isEditingRow,
    autofocus: true,
  };
};
