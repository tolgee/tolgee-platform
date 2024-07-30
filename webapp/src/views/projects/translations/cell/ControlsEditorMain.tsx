import React, { useRef, useState } from 'react';
import { T } from '@tolgee/react';
import { Button, ButtonGroup, Menu, MenuItem, styled } from '@mui/material';
import { ArrowDropDown } from '@mui/icons-material';

import LoadingButton from 'tg.component/common/form/LoadingButton';

import { useTranslationsSelector } from '../context/TranslationsContext';
import { SaveProps } from '../useTranslationCell';

const StyledContainer = styled('div')`
  display: flex;
  align-items: center;
  gap: 12px;
`;

const StyledButton = styled(Button)`
  min-width: 0px !important;
  padding: 0px 2px;
`;

type ControlsProps = {
  onSave?: (options: SaveProps) => void;
  onCancel?: () => void;
  className?: string;
  assignedTaskId: number | undefined;
};

export const ControlsEditorMain: React.FC<ControlsProps> = ({
  onSave,
  onCancel,
  className,
  assignedTaskId,
}) => {
  const isEditLoading = useTranslationsSelector((c) => c.isEditLoading);
  const anchorEl = useRef<HTMLElement>(null);
  const [open, setOpen] = useState(false);

  const withClose = (callback?: () => void) => () => {
    setOpen(false);
    callback?.();
  };

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
      {typeof assignedTaskId === 'number' ? (
        <>
          <ButtonGroup size="small" ref={anchorEl as any}>
            <LoadingButton
              onClick={() => onSave?.({ resolveTaskId: assignedTaskId })}
              color="primary"
              variant="contained"
              loading={isEditLoading}
              data-cy="translations-cell-save-button"
            >
              <T keyName="translations_cell_save_and_done" />
            </LoadingButton>
            <StyledButton
              color="primary"
              variant="contained"
              onClick={() => setOpen(true)}
            >
              <ArrowDropDown />
            </StyledButton>
          </ButtonGroup>
          <Menu
            id="basic-menu"
            anchorEl={anchorEl.current}
            open={open}
            onClose={withClose()}
            MenuListProps={{
              'aria-labelledby': 'basic-button',
            }}
            anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
            transformOrigin={{ horizontal: 'right', vertical: 'top' }}
          >
            <MenuItem onClick={withClose(() => onSave?.({}))}>
              <T keyName="translations_cell_save_only" />
            </MenuItem>
          </Menu>
        </>
      ) : (
        <LoadingButton
          onClick={() => onSave?.({})}
          color="primary"
          size="small"
          variant="contained"
          loading={isEditLoading}
          data-cy="translations-cell-save-button"
        >
          <T keyName="translations_cell_save" />
        </LoadingButton>
      )}
    </StyledContainer>
  );
};
