import { Box } from '@material-ui/core';
import React from 'react';
import { useState } from 'react';
import { useContextSelector } from 'use-context-selector';

import {
  TranslationsContext,
  useTranslationsDispatch,
} from './TranslationsContext';

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

  const handleSave = () => {
    dispatch({
      type: 'CHANGE_FIELD',
      payload: {
        keyId,
        keyName,
        language,
        value,
        after: 'GO_TO_NEXT_KEY',
      },
    });
  };

  return (
    <Box
      onClick={switchToEdit}
      width="100%"
      minHeight="1.5em"
      whiteSpace="nowrap"
      overflow="hidden"
      textOverflow="ellipsis"
    >
      {isEditing ? (
        <input
          autoFocus
          style={{
            width: '100%',
          }}
          value={value}
          onChange={(e) => setValue(e.currentTarget.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSave()}
        />
      ) : (
        text
      )}
    </Box>
  );
});
