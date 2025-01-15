import React, { useRef } from 'react';
import { T } from '@tolgee/react';
import { CameraPlus, Edit02 } from '@untitled-ui/icons-react';
import { styled } from '@mui/material';

import { CELL_SHOW_ON_HOVER } from './styles';
import { ControlsButton } from './ControlsButton';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { ScreenshotUploadInput } from './ScreenshotUploadInput';

const StyledControls = styled('div')`
  display: flex;
  gap: 12px;
`;

type ControlsProps = {
  editEnabled?: boolean;
  onEdit?: () => void;
  showScreenshotsAddition: boolean;
  keyId: number;
};

export const ControlsKey: React.FC<ControlsProps> = ({
  editEnabled,
  onEdit,
  showScreenshotsAddition,
  keyId,
}) => {
  const { satisfiesPermission } = useProjectPermissions();
  const canViewScreenshots = satisfiesPermission('screenshots.view');
  const openFileDialogRef = useRef<() => void>(null);

  // right section
  const displayEdit = editEnabled && onEdit;
  const displayScreenshots = showScreenshotsAddition && canViewScreenshots;

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
        <>
          <ScreenshotUploadInput keyId={keyId} handlerRef={openFileDialogRef} />
          <ControlsButton
            tooltip={<T keyName="translations_screenshots_add_tooltip" />}
            data-cy="translations-cell-screenshots-button"
            className={CELL_SHOW_ON_HOVER}
            onClick={() => openFileDialogRef.current?.()}
          >
            <CameraPlus />
          </ControlsButton>
        </>
      )}
    </StyledControls>
  );
};
