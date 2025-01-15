import clsx from 'clsx';
import React, { FunctionComponent, useEffect, useState } from 'react';
import { Box, styled } from '@mui/material';
import { green, red } from '@mui/material/colors';
import { XCircle } from '@untitled-ui/icons-react';

import { FileUploadFixtures } from 'tg.fixtures/FileUploadFixtures';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

import { Dropzone } from 'tg.component/CustomIcons';
import { MAX_FILE_COUNT } from './Screenshots';

export interface ScreenshotDropzoneProps {
  validateAndUpload: (files: File[]) => void;
}

const StyledDropZoneValidation = styled(Box)`
  pointer-events: none;
  &.valid {
    opacity: 1;
  }

  &.invalid {
    border: 1px solid ${red[200]};
    background-color: ${red[50]};
    backdrop-filter: blur(5px);
  }
`;

const StyledValidIcon = styled(Dropzone)`
  filter: drop-shadow(1px 1px 0px ${green[200]})
    drop-shadow(-1px 1px 0px ${green[200]})
    drop-shadow(1px -1px 0px ${green[200]})
    drop-shadow(-1px -1px 0px ${green[200]});
  width: 100px;
  height: 100px;
  color: ${({ theme }) => theme.palette.common.white};
`;

const StyledInvalidIcon = styled(XCircle)`
  filter: drop-shadow(1px 1px 0px ${red[200]})
    drop-shadow(-1px 1px 0px ${red[200]}) drop-shadow(1px -1px 0px ${red[200]})
    drop-shadow(-1px -1px 0px ${red[200]});
  width: 100px;
  height: 100px;
  color: ${({ theme }) => theme.palette.common.white};
`;

export const ScreenshotDropzone: FunctionComponent<ScreenshotDropzoneProps> = ({
  validateAndUpload,
  ...props
}) => {
  const [dragOver, setDragOver] = useState(null as null | 'valid' | 'invalid');
  const [dragEnterTarget, setDragEnterTarget] = useState(
    null as EventTarget | null
  );
  const projectPermissions = useProjectPermissions();

  useEffect(() => {
    const listener = (e) => {
      e.preventDefault();
    };
    window.addEventListener('dragover', listener);
    window.addEventListener('drop', listener);
    return () => {
      window.removeEventListener('dragover', listener);
      window.removeEventListener('drop', listener);
    };
  }, []);

  const onDragEnter = (e: React.DragEvent) => {
    setDragEnterTarget(e.target);
    if (e.dataTransfer.items) {
      const files = FileUploadFixtures.dataTransferItemsToArray(
        e.dataTransfer.items
      );
      if (files.length > MAX_FILE_COUNT) {
        setDragOver('invalid');
        return;
      }
      setDragOver('valid');
    }
  };

  const onDragLeave = (e: React.DragEvent) => {
    if (e.target === dragEnterTarget) {
      setDragOver(null);
    }
  };

  const onDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    if (e.dataTransfer.items) {
      const files = FileUploadFixtures.dataTransferItemsToArray(
        e.dataTransfer.items
      );
      validateAndUpload(files);
    }
    setDragOver(null);
  };

  let dropZoneAllowedProps = {};
  if (projectPermissions.satisfiesPermission('screenshots.upload')) {
    dropZoneAllowedProps = { onDrop, onDragEnter, onDragLeave };
  }

  return (
    <>
      <Box
        position="relative"
        display="grid"
        {...dropZoneAllowedProps}
        overflow="visible"
        data-cy="dropzone"
      >
        <StyledDropZoneValidation
          zIndex={2}
          position="absolute"
          width="100%"
          height="100%"
          className={clsx({
            valid: dragOver === 'valid',
            invalid: dragOver === 'invalid',
          })}
          display="flex"
          alignItems="center"
          justifyContent="center"
        >
          {dragOver === 'valid' && <StyledValidIcon />}
          {dragOver === 'invalid' && <StyledInvalidIcon />}
        </StyledDropZoneValidation>
        {props.children}
      </Box>
    </>
  );
};
