import { useState, useEffect, useRef } from 'react';
import { useContextSelector } from 'use-context-selector';

import {
  AfterCommand,
  TranslationsContext,
  useTranslationsDispatch,
} from './context/TranslationsContext';
import { EditModeType } from './context/useEdit';

type Props = {
  keyId: number;
  keyName: string;
  language: string | undefined;
  defaultVal?: string;
  onSaveSuccess?: (val: string) => void;
};

export const useEditableRow = ({
  keyId,
  keyName,
  defaultVal,
  language,
  onSaveSuccess,
}: Props) => {
  const edit = useContextSelector(TranslationsContext, (v) => {
    // find language or keyName (in case of undefined)
    return v.edit?.keyId === keyId && v.edit.language === language
      ? v.edit
      : undefined;
  });

  const originalValue = defaultVal || '';

  const [value, setValue] = useState(originalValue);

  const isEditing = Boolean(edit);

  const dispatch = useTranslationsDispatch();

  useEffect(() => {
    if (isEditing) {
      setValue(edit?.savedValue || originalValue);
    }
  }, [isEditing, originalValue]);

  const handleOpen = (mode: EditModeType) => {
    dispatch({
      type: 'SET_EDIT',
      payload: {
        keyId,
        keyName,
        language,
        mode,
      },
    });
  };

  const handleSave = (after?: AfterCommand) => {
    dispatch({
      type: 'CHANGE_FIELD',
      payload: {
        keyId,
        keyName,
        language,
        value,
        after,
        onSuccess: () => onSaveSuccess?.(value),
      },
    });
  };

  const handleClose = () => {
    dispatch({ type: 'SET_EDIT', payload: undefined });
  };

  const handleModeChange = (mode: EditModeType) => {
    dispatch({ type: 'UPDATE_EDIT', payload: { mode: mode } });
  };

  useEffect(() => {
    const isChanged = originalValue !== value;
    // let context know, that something has changed
    if (edit && Boolean(edit?.changed) !== isChanged) {
      dispatch({ type: 'UPDATE_EDIT', payload: { changed: isChanged } });
    }
  }, [originalValue, edit?.changed, value]);

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
    handleOpen,
    handleClose,
    handleSave,
    handleModeChange,
    value,
    setValue,
    editVal: edit,
    isEditing,
    autofocus: edit?.savedValue === undefined,
  };
};
