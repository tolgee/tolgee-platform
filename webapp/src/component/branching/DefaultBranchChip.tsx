import { Chip } from '@mui/material';
import React from 'react';
import { useTranslate } from '@tolgee/react';

export const DefaultBranchChip = () => {
  const { t } = useTranslate();
  return <Chip size={'small'} label={t('default_branch')} />;
};
