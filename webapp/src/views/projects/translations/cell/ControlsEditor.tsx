import React from 'react';
import { T } from '@tolgee/react';
import { Button, styled } from '@mui/material';
import { CameraAlt } from '@mui/icons-material';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { components } from 'tg.service/apiSchema.generated';
import { StateType } from 'tg.constants/translationStates';
import { ControlsButton } from './ControlsButton';
import { StateTransitionButtons } from './StateTransitionButtons';
import { useTranslationsSelector } from '../context/TranslationsContext';

type State = components['schemas']['TranslationViewModel']['state'];

const StyledLeftPart = styled('div')`
  display: flex;
  align-items: flex-start;
  overflow: hidden;
  padding: ${({ theme }) => theme.spacing(1, 1.5, 1.5, 1.5)};
  gap: 10px;
`;

const StyledRightPart = styled('div')`
  display: flex;
  align-items: center;
  padding: ${({ theme }) => theme.spacing(1, 1.5, 1.5, 0)};
  gap: 8px;
`;

type ControlsProps = {
  state?: State;
  onSave?: () => void;
  onCancel?: () => void;
  onScreenshots?: () => void;
  onStateChange?: (state: StateType) => void;
  screenshotRef?: React.Ref<any>;
  screenshotsPresent?: boolean;
};

export const ControlsEditor: React.FC<ControlsProps> = ({
  state,
  onSave,
  onCancel,
  onScreenshots,
  onStateChange,
  screenshotRef,
  screenshotsPresent,
}) => {
  // right section
  const displayTransitionButtons = state;
  const displayScreenshots = onScreenshots;
  const displayRightPart = displayTransitionButtons || displayScreenshots;

  const isEditLoading = useTranslationsSelector((c) => c.isEditLoading);

  return (
    <>
      <StyledLeftPart>
        <Button
          onClick={onCancel}
          color="primary"
          variant="outlined"
          size="small"
          data-cy="translations-cell-cancel-button"
        >
          <T>translations_cell_cancel</T>
        </Button>
        <LoadingButton
          onClick={onSave}
          color="primary"
          variant="contained"
          size="small"
          loading={isEditLoading}
          data-cy="translations-cell-save-button"
        >
          <T>translations_cell_save</T>
        </LoadingButton>
      </StyledLeftPart>

      {displayRightPart && (
        <StyledRightPart>
          {displayTransitionButtons && (
            <StateTransitionButtons
              state={state}
              onStateChange={onStateChange}
            />
          )}
          {displayScreenshots && (
            <ControlsButton
              onClick={onScreenshots}
              ref={screenshotRef}
              tooltip={<T>translations_screenshots_tooltip</T>}
              data-cy="translations-cell-screenshots-button"
            >
              <CameraAlt
                fontSize="small"
                color={screenshotsPresent ? 'primary' : undefined}
              />
            </ControlsButton>
          )}
        </StyledRightPart>
      )}
    </>
  );
};
