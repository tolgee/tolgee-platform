import { useState, useEffect, useRef } from 'react';
import { useContextSelector } from 'use-context-selector';

import { DirectionType } from 'tg.component/editor/Editor';
import { components } from 'tg.service/apiSchema.generated';
import {
  TranslationsContext,
  useTranslationsDispatch,
} from './TranslationsContext';

type TranslationModel = components['schemas']['TranslationModel'];

type Props = {
  keyId: number;
  keyName: string;
  language: string | undefined | null;
  translations?: Record<string, TranslationModel>;
  defaultVal?: string;
};

export const useEditableRow = ({
  keyId,
  keyName,
  defaultVal,
  translations,
  language,
}: Props) => {
  const edit = useContextSelector(TranslationsContext, (v) => {
    // in case of null, listen for whole row
    if (language === null) {
      return v.edit?.keyId === keyId ? v.edit : undefined;
    } else {
      // otherwise identify language or keyName (in case of undefined)
      return v.edit?.keyId === keyId && v.edit.language === language
        ? v.edit
        : undefined;
    }
  });

  const originalValue =
    (edit &&
      (edit.savedValue ||
        (edit.language && translations?.[edit.language]?.text) ||
        defaultVal)) ||
    '';

  const [value, setValue] = useState(originalValue);

  const isEditing = Boolean(edit);

  const dispatch = useTranslationsDispatch();

  useEffect(() => {
    if (isEditing) {
      setValue(originalValue);
    }
  }, [isEditing]);

  const handleEdit = (language?: string) => {
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
        language: edit?.language,
        value,
        after: direction,
      },
    });
  };

  const handleEditCancel = () => {
    dispatch({ type: 'SET_EDIT', payload: undefined });
  };

  useEffect(() => {
    const isChanged = originalValue !== value;
    // let context know if value is different from original
    if (isEditing && edit?.changed !== isChanged) {
      dispatch({ type: 'UPDATE_EDIT', payload: { changed: isChanged } });
    }
  }, [edit, value, originalValue]);

  const valueRef = useRef(isEditing ? value : undefined);

  useEffect(() => {
    valueRef.current = isEditing ? value : undefined;
  }, [value, isEditing]);

  useEffect(() => {
    return () => {
      // on unmount store value in context, so it won't get lost
      if (valueRef.current !== undefined) {
        dispatch({
          type: 'UPDATE_EDIT',
          payload: { savedValue: valueRef.current },
        });
      }
    };
  }, [valueRef]);

  return {
    handleEdit,
    handleSave,
    handleEditCancel,
    value,
    setValue,
    editVal: edit,
    isEditing,
  };
};
