import { IconButton, Tooltip } from '@mui/material';
import { T } from '@tolgee/react';
import { Copy06, Check } from '@untitled-ui/icons-react';
import React, { ReactElement } from 'react';

import { useCopyWithFeedback } from 'tg.hooks/useCopyWithFeedback';

type Props = {
  tooltip?: string | ReactElement;
  value: () => string;
  successMessage?: string | ReactElement;
  icon?: ReactElement;
};

export const ClipboardCopy = ({
  tooltip,
  value,
  successMessage,
  icon,
}: Props) => {
  const { copied, copyText } = useCopyWithFeedback();
  const handleClick = () => copyText(value());

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
          icon ?? <Copy06 height={20} width={20} />
        )}
      </IconButton>
    </Tooltip>
  );
};
