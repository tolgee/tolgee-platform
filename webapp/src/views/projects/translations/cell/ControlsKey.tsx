import React from 'react';
import { T } from '@tolgee/react';
import { Camera01, Edit02 } from '@untitled-ui/icons-react';
import { styled, useTheme } from '@mui/material';

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
  onScreenshots?: () => void;
  screenshotRef?: React.Ref<any>;
  screenshotsPresent?: boolean;
  screenshotsOpen?: boolean;
};

export const ControlsKey: React.FC<ControlsProps> = ({
  editEnabled,
  onEdit,
  onScreenshots,
  screenshotRef,
  screenshotsPresent,
  screenshotsOpen,
}) => {
  const { satisfiesPermission } = useProjectPermissions();
  const canViewScreenshots = satisfiesPermission('screenshots.view');
  const theme = useTheme();

  // right section
  const displayEdit = editEnabled && onEdit;
  const displayScreenshots = onScreenshots && canViewScreenshots;

  return (
    <StyledControls>
      {displayEdit && (
        <ControlsButton
          onClick={onEdit}
          data-cy="translations-cell-edit-button"
          className={CELL_SHOW_ON_HOVER}
          tooltip={<T keyName="translations_cell_edit" />}
        >
          <Edit02 />
        </ControlsButton>
      )}
      {displayScreenshots && (
        <ControlsButton
          onClick={onScreenshots}
          ref={screenshotRef}
          tooltip={<T keyName="translations_screenshots_tooltip" />}
          data-cy="translations-cell-screenshots-button"
          className={
            screenshotsPresent || screenshotsOpen
              ? undefined
              : CELL_SHOW_ON_HOVER
          }
        >
          <Camera01
            color={screenshotsPresent ? theme.palette.primary.main : undefined}
          />
        </ControlsButton>
      )}
    </StyledControls>
  );
};
