import React from 'react';
import { useTranslate } from '@tolgee/react';
import { CameraPlus, Edit02 } from '@untitled-ui/icons-react';
import { styled } from '@mui/material';

import { CELL_SHOW_ON_HOVER } from './styles';
import { ControlsButton } from './ControlsButton';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

const StyledControls = styled('div')`
  display: flex;
  gap: 12px;
`;

type ControlsProps = {
  editEnabled?: boolean;
  onEdit?: () => void;
  onAddScreenshot?: () => void;
};

export const ControlsKey: React.FC<ControlsProps> = ({
  editEnabled,
  onEdit,
  onAddScreenshot,
}) => {
  const { satisfiesPermission } = useProjectPermissions();
  const canViewScreenshots = satisfiesPermission('screenshots.view');

  // right section
  const displayEdit = editEnabled && onEdit;
  const displayScreenshots = onAddScreenshot && canViewScreenshots;

  const { t } = useTranslate();

  return (
    <StyledControls>
      {displayEdit && (
        <ControlsButton
          onClick={onEdit}
          data-cy="translations-cell-edit-button"
          className={CELL_SHOW_ON_HOVER}
          tooltip={t('translations_cell_edit')}
        >
          <Edit02 />
        </ControlsButton>
      )}
      {displayScreenshots && (
        <>
          <ControlsButton
            tooltip={t('translations_screenshots_add_tooltip')}
            data-cy="translations-cell-screenshots-button"
            className={CELL_SHOW_ON_HOVER}
            onClick={onAddScreenshot}
          >
            <CameraPlus />
          </ControlsButton>
        </>
      )}
    </StyledControls>
  );
};
