import React from 'react';
import { useState } from 'react';
import { useContextSelector } from 'use-context-selector';

import { Editor, DirectionType } from 'tg.component/editor/Editor';

import {
  TranslationsContext,
  useTranslationsDispatch,
} from './TranslationsContext';
import { useEffect } from 'react';
import { CellPlain } from './CellPlain';

type Props = {
  text: string;
  keyId: number;
  keyName: string;
  language: string | undefined;
};

export const Cell: React.FC<Props> = React.memo(function Cell({
  text,
  keyName,
  language,
  keyId,
}) {
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

  const switchToEdit = () => {
    dispatch({
      type: 'SET_EDIT',
      payload: {
        keyId,
        keyName,
        language,
      },
    });
  };

  const handleSave = (direction: DirectionType) => {
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

  return (
    <CellPlain
      onClick={switchToEdit}
      background={isEditing ? '#efefef' : undefined}
    >
      {isEditing ? (
        <Editor
          minHeight={100}
          initialValue={value}
          variables={[]}
          onChange={(v) => setValue(v as string)}
          onSave={handleSave}
          onCancel={handleEditCancel}
          autoFocus
        />
      ) : (
        text
      )}
    </CellPlain>
  );
});
