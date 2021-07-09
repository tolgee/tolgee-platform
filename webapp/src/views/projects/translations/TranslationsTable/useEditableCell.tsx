import { useState, useEffect } from 'react';
import { useContextSelector } from 'use-context-selector';
import { DirectionType } from 'tg.component/editor/Editor';

import {
  TranslationsContext,
  useTranslationsDispatch,
} from '../TranslationsContext';

type Props = {
  text: string;
  language?: string;
  keyId: number;
  keyName: string;
};

export const useEditableCell = ({ text, language, keyId, keyName }: Props) => {
  const [value, setValue] = useState(text || '');

  const isEditing = useContextSelector(
    TranslationsContext,
    (v) => v.edit?.keyId === keyId && v.edit?.language === language
  );

  const dispatch = useTranslationsDispatch();

  useEffect(() => {
    if (isEditing) {
      setValue(text);
    }
  }, [isEditing]);

  const handleEdit = () => {
    dispatch({
      type: 'SET_EDIT',
      payload: {
        keyId,
        keyName,
        language,
      },
    });
  };

  const handleSave = (direction?: DirectionType) => {
    dispatch({
      type: 'CHANGE_FIELD',
      payload: {
        keyId,
        keyName,
        language,
        value,
        after: direction,
      },
    });
  };

  const handleEditCancel = () => {
    dispatch({ type: 'SET_EDIT', payload: undefined });
  };

  return {
    handleEdit,
    handleSave,
    handleEditCancel,
    value,
    setValue,
    isEditing,
  };
};
