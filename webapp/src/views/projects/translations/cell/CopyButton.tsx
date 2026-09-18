import React, { useState } from 'react';
import copy from 'copy-to-clipboard';
import { Check, Copy06 } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';

import { ControlsButton } from 'tg.views/projects/translations/cell/ControlsButton';

type Props = Omit<
  React.ComponentProps<typeof ControlsButton>,
  'onClick' | 'tooltip' | 'children'
> & {
  text: string;
};

export const CopyButton = ({ text, ...props }: Props) => {
  const [copied, setCopied] = useState(false);
  const { t } = useTranslate();

  const handleClick = () => {
    if (copy(text)) {
      setCopied(true);
    }
  };

  return (
    <ControlsButton
      {...props}
      onClick={handleClick}
      data-cy-copied={String(copied)}
      tooltip={copied ? t('copied_to_clipboard') : t('clipboard_copy')}
    >
      {copied ? <Check /> : <Copy06 />}
    </ControlsButton>
  );
};
