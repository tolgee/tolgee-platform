import React, { useEffect, useRef } from 'react';
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
  cellRef: React.RefObject<HTMLElement>;
};

export const useEditableRow = ({
  keyId,
  defaultVal,
  language,
  onSaveSuccess,
  cellRef,
}: Props) => {
  const dispatch = useTranslationsDispatch();

  const cursor = useContextSelector(TranslationsContext, (v) => {
    // find language or keyName (in case of undefined)
    return v.cursor?.keyId === keyId && v.cursor.language === language
      ? v.cursor
      : undefined;
  });

  const value = cursor?.savedValue || '';

  const originalValue = defaultVal || '';

  const setValue = (val: string) =>
    dispatch({ type: 'UPDATE_EDIT', payload: { savedValue: val } });

  const isEditing = Boolean(cursor);

  useEffect(() => {
    dispatch({
      type: 'REGISTER_ELEMENT',
      payload: { keyId, language, ref: cellRef.current! },
    });
    return () =>
      dispatch({
        type: 'UNREGISTER_ELEMENT',
        payload: { keyId, language, ref: cellRef.current! },
      });
  }, [cellRef.current, keyId, language]);

  useEffect(() => {
    if (isEditing) {
      setValue(cursor?.savedValue || originalValue);
    }
  }, [isEditing, originalValue]);

  const handleOpen = (mode: EditModeType) => {
    dispatch({
      type: 'SET_EDIT',
      payload: {
        keyId,
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
    if (cursor && Boolean(cursor?.changed) !== isChanged) {
      dispatch({ type: 'UPDATE_EDIT', payload: { changed: isChanged } });
    }
  }, [originalValue, cursor?.changed, value]);

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
    editVal: isEditing ? cursor : undefined,
    isEditing,
    autofocus: true,
  };
};
