import React from 'react';
import { Edit, CameraAlt } from '@material-ui/icons';
import { T } from '@tolgee/react';

import { useCellStyles } from './styles';
import { ControlsButton } from './ControlsButton';

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
  const cellClasses = useCellStyles({});

  // right section
  const displayEdit = editEnabled && onEdit;
  const displayScreenshots = onScreenshots;

  return (
    <>
      {displayEdit && (
        <ControlsButton
          onClick={onEdit}
          data-cy="translations-cell-edit-button"
          className={cellClasses.showOnHover}
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
              : cellClasses.showOnHover
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
