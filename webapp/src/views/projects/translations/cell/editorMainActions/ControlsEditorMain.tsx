import React, { useRef, useState } from 'react';
import { T } from '@tolgee/react';
import { Button, ButtonGroup, Menu, MenuItem, styled } from '@mui/material';
import { ArrowDropDown } from 'tg.component/CustomIcons';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { components } from 'tg.service/apiSchema.generated';

import { useTranslationsSelector } from '../../context/TranslationsContext';
import { SaveProps } from '../../useTranslationCell';
import { useProject } from 'tg.hooks/useProject';
import { getEditorActions } from './getEditorActions';
import { TolgeeFormat } from '@tginternal/editor';

type TaskModel = components['schemas']['KeyTaskViewModel'];
type TranslationViewModel = components['schemas']['TranslationViewModel'];

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
  onSave: (options?: SaveProps) => void;
  onCancel: () => void;
  className?: string;
  tasks: TaskModel[] | undefined;
  currentTask: number | undefined;
  translation: TranslationViewModel | undefined;
  languageId: number;
  value: TolgeeFormat;
};

export const ControlsEditorMain: React.FC<ControlsProps> = ({
  onSave,
  onCancel,
  className,
  tasks,
  currentTask,
  translation,
  languageId,
  value,
}: ControlsProps) => {
  const project = useProject();

  const actions = getEditorActions({
    onSave,
    translation,
    languageId,
    tasks,
    currentTask,
    project,
    value,
  });

  const isEditLoading = useTranslationsSelector((c) => c.isEditLoading);
  const anchorEl = useRef<HTMLElement>(null);
  const [open, setOpen] = useState(false);

  const withClose = (callback?: () => void) => () => {
    setOpen(false);
    callback?.();
  };

  const [firstAction, ...otherActions] = actions;

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
      {actions.length > 1 ? (
        <>
          <ButtonGroup size="small" ref={anchorEl as any}>
            <LoadingButton
              onClick={() => firstAction.action()}
              disabled={firstAction.disabled}
              color="primary"
              variant="contained"
              loading={isEditLoading}
              data-cy="translations-cell-main-action-button"
              sx={{
                whiteSpace: 'nowrap',
              }}
            >
              {firstAction.label}
            </LoadingButton>
            <StyledButton
              color="primary"
              variant="contained"
              onClick={() => setOpen(true)}
              data-cy="translations-cell-menu-open-button"
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
            {otherActions.map((action, i) => (
              <MenuItem
                key={i}
                onClick={withClose(action.action)}
                disabled={action.disabled}
                data-cy="translations-cell-menu-item"
              >
                {action.label}
              </MenuItem>
            ))}
          </Menu>
        </>
      ) : (
        <LoadingButton
          onClick={() => firstAction?.action()}
          disabled={firstAction.disabled}
          color="primary"
          size="small"
          variant="contained"
          loading={isEditLoading}
          data-cy="translations-cell-main-action-button"
        >
          {firstAction?.label}
        </LoadingButton>
      )}
    </StyledContainer>
  );
};
