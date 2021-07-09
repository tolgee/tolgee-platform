import { useState, useEffect } from 'react';
import { useContextSelector } from 'use-context-selector';

import { DirectionType } from 'tg.component/editor/Editor';
import { components } from 'tg.service/apiSchema.generated';
import {
  TranslationsContext,
  useTranslationsDispatch,
} from '../TranslationsContext';

type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

export const useEditableRow = ({
  keyId,
  keyName,
  translations,
}: KeyWithTranslationsModel) => {
  const [value, setValue] = useState('');

  const edit = useContextSelector(TranslationsContext, (v) =>
    v.edit?.keyId === keyId && v.edit.language !== undefined
      ? v.edit
      : undefined
  );

  const dispatch = useTranslationsDispatch();

  useEffect(() => {
    if (edit) {
      setValue(translations[edit!.language!]?.text || '');
    }
  }, [edit]);

  const handleEdit = (language: string) => {
    dispatch({
      type: 'SET_EDIT',
      payload: {
        keyId,
        keyName,
        language,
      },
    });
  };

  const handleSave = (language: string, direction?: DirectionType) => {
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
    languageEdited: edit?.language,
  };
};
