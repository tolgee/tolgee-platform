import React from 'react';
import { T } from '@tolgee/react';
import { Button, styled } from '@mui/material';

import LoadingButton from 'tg.component/common/form/LoadingButton';

import { useTranslationsSelector } from '../context/TranslationsContext';

const StyledContainer = styled('div')`
  display: flex;
  align-items: center;
  gap: 12px;
`;

type ControlsProps = {
  onSave?: () => void;
  onCancel?: () => void;
  className?: string;
};

export const ControlsEditorMain: React.FC<ControlsProps> = ({
  onSave,
  onCancel,
  className,
}) => {
  const isEditLoading = useTranslationsSelector((c) => c.isEditLoading);

  return (
    <StyledContainer className={className}>
      <Button
        onClick={onCancel}
        color="primary"
        variant="outlined"
        size="small"
        data-cy="translations-cell-cancel-button"
      >
        <T keyName="translations_cell_cancel" />
      </Button>
      <LoadingButton
        onClick={onSave}
        color="primary"
        variant="contained"
        size="small"
        loading={isEditLoading}
        data-cy="translations-cell-save-button"
      >
        <T keyName="translations_cell_save" />
      </LoadingButton>
    </StyledContainer>
  );
};
