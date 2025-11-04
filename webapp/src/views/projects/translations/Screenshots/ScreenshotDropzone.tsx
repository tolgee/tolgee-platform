import React, { FunctionComponent, useEffect } from 'react';
import { DragDropArea } from 'tg.component/common/DragDropArea';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

import { MAX_FILE_COUNT } from './Screenshots';
import { FilesType } from 'tg.fixtures/FileUploadFixtures';

export interface ScreenshotDropzoneProps {
  validateAndUpload: (files: FilesType) => void;
}

export const ScreenshotDropzone: FunctionComponent<ScreenshotDropzoneProps> = ({
  validateAndUpload,
  ...props
}) => {
  const projectPermissions = useProjectPermissions();

  useEffect(() => {
    const listener = (e: Event) => {
      e.preventDefault();
    };
    window.addEventListener('dragover', listener);
    window.addEventListener('drop', listener);
    return () => {
      window.removeEventListener('dragover', listener);
      window.removeEventListener('drop', listener);
    };
  }, []);

  return (
    <DragDropArea
      onFilesReceived={validateAndUpload}
      onClick={() => {}}
      active={projectPermissions.satisfiesPermission('screenshots.upload')}
      maxItems={MAX_FILE_COUNT}
      showOverlay
    >
      {props.children}
    </DragDropArea>
  );
};
