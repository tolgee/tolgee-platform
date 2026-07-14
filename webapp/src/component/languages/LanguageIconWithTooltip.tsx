import React, { FC } from 'react';
import { Tooltip } from '@mui/material';
import { CircledLanguageIcon } from './CircledLanguageIcon';
import { LanguageReferenceType } from '../activity/types';

type LanguageIconWithTooltipProps = {
  l: LanguageReferenceType;
};

export const LanguageIconWithTooltip: FC<LanguageIconWithTooltipProps> = ({
  l,
}) => {
  return (
    <Tooltip title={`${l.name} (${l.tag})`}>
      <span>
        <CircledLanguageIcon size={14} flag={l.flagEmoji} />
      </span>
    </Tooltip>
  );
};
