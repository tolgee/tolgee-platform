import { IconButton, Tooltip } from '@mui/material';
import { T } from '@tolgee/react';
import copy from 'copy-to-clipboard';
import { Copy06, Check } from '@untitled-ui/icons-react';
import React, { ReactElement, useEffect, useRef, useState } from 'react';

type Props = {
  tooltip?: string | ReactElement;
  value: () => string;
  successMessage?: string | ReactElement;
};

export const ClipboardCopy = ({ tooltip, value, successMessage }: Props) => {
  const [copied, setCopied] = useState(false);
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  const handleClick = () => {
    copy(value() || '');
    setCopied(true);

    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }
    timerRef.current = setTimeout(() => {
      setCopied(false);
    }, 2000); // reset after 2s
  };

  useEffect(() => {
    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
    };
  }, []);

  return (
    <Tooltip
      title={
        copied
          ? successMessage || <T keyName="copied_to_clipboard" />
          : tooltip || <T keyName="clipboard_copy" />
      }
    >
      <IconButton onClick={handleClick}>
        {copied ? (
          <Check height={20} width={20} />
        ) : (
          <Copy06 height={20} width={20} />
        )}
      </IconButton>
    </Tooltip>
  );
};
