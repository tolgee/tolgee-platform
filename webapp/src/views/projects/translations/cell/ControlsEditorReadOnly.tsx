import React from 'react';
import { T } from '@tolgee/react';
import { Button, styled } from '@mui/material';

const StyledContainer = styled('div')`
  display: flex;
  align-items: center;
  gap: 12px;
`;

type ControlsProps = {
  onClose?: () => void;
  className?: string;
};

export const ControlsEditorReadOnly: React.FC<ControlsProps> = ({
  onClose,
  className,
}) => {
  return (
    <StyledContainer className={className}>
      <Button
        onClick={onClose}
        color="primary"
        variant="outlined"
        size="small"
        data-cy="translations-cell-cancel-button"
      >
        <T keyName="translations_cell_close" />
      </Button>
    </StyledContainer>
  );
};
