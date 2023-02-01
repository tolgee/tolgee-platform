import React from 'react';
import { Edit, CameraAlt } from '@mui/icons-material';
import { T } from '@tolgee/react';

import { CELL_SHOW_ON_HOVER } from './styles';
import { ControlsButton } from './ControlsButton';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

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

  // right section
  const displayEdit = editEnabled && onEdit;
  const displayScreenshots = onScreenshots && canViewScreenshots;

  return (
    <>
      {displayEdit && (
        <ControlsButton
          onClick={onEdit}
          data-cy="translations-cell-edit-button"
          className={CELL_SHOW_ON_HOVER}
          tooltip={<T>translations_cell_edit</T>}
        >
          <Edit fontSize="small" />
        </ControlsButton>
      )}
      {displayScreenshots && (
        <ControlsButton
          onClick={onScreenshots}
          ref={screenshotRef}
          tooltip={<T>translations_screenshots_tooltip</T>}
          data-cy="translations-cell-screenshots-button"
          className={
            screenshotsPresent || screenshotsOpen
              ? undefined
              : CELL_SHOW_ON_HOVER
          }
        >
          <CameraAlt
            fontSize="small"
            color={screenshotsPresent ? 'primary' : undefined}
          />
        </ControlsButton>
      )}
    </>
  );
};
