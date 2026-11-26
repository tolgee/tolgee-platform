import React from 'react';
import { Check, Copy06 } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';

import { useCopyWithFeedback } from 'tg.hooks/useCopyWithFeedback';
import { ControlsButton } from 'tg.views/projects/translations/cell/ControlsButton';

type Props = {
  text: string;
  style?: React.CSSProperties;
  className?: string;
};

export const CopyTranslationButton = ({ text, style, className }: Props) => {
  const { t } = useTranslate();
  const { copied, copyText } = useCopyWithFeedback();

  const handleCopy = (e: React.MouseEvent<HTMLButtonElement>) => {
    copyText(text);
    const isPointerClick = e.detail > 0;
    if (isPointerClick) {
      e.currentTarget.blur();
    }
  };

  return (
    <ControlsButton
      style={style}
      onClick={handleCopy}
      data-cy="translations-cell-copy-button"
      data-cy-copied={copied}
      className={className}
      tooltip={
        copied
          ? t('copied_to_clipboard', 'Copied!')
          : t('translations_cell_copy', 'Copy to clipboard')
      }
    >
      {copied ? <Check /> : <Copy06 />}
    </ControlsButton>
  );
};
