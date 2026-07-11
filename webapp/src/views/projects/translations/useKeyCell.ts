import React, { useEffect } from 'react';

import {
  useTranslationsSelector,
  useTranslationsActions,
} from './context/TranslationsContext';
import {
  DeletableKeyWithTranslationsModelType,
  EditMode,
} from './context/types';

type Props = {
  keyData: DeletableKeyWithTranslationsModelType;
  cellRef: React.RefObject<HTMLElement>;
};

export const useKeyCell = ({ keyData, cellRef }: Props) => {
  const {
    setEditValue,
    setEditValueString,
    registerElement,
    unregisterElement,
    setEdit,
    setEditForce,
  } = useTranslationsActions();

  const keyId = keyData.keyId;

  const cursor = useTranslationsSelector((v) => {
    return v.cursor?.keyId === keyId ? v.cursor : undefined;
  });

  const isEditingRow = Boolean(cursor?.keyId === keyId);
  const isEditing = Boolean(isEditingRow && cursor?.language === undefined);
  const isEditingTranslation = Boolean(isEditingRow && cursor?.language);

  useEffect(() => {
    registerElement({ keyId, language: undefined, ref: cellRef.current! });
    return () => {
      unregisterElement({ keyId, language: undefined, ref: cellRef.current! });
    };
  }, [cellRef.current, keyId]);

  const handleOpen = (mode?: EditMode) => {
    setEdit({
      keyId,
      language: undefined,
      mode,
    });
  };

  const handleClose = (force = false) => {
    if (force) {
      setEditForce(undefined);
    } else {
      setEdit(undefined);
    }
  };

  return {
    keyId,
    handleOpen,
    handleClose,
    setEditValue,
    setEditValueString,
    editVal: isEditing ? cursor : undefined,
    isEditing,
    isEditingRow,
    isEditingTranslation,
    autofocus: true,
    keyData,
  };
};
