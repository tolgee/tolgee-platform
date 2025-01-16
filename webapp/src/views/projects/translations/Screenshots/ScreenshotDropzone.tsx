import clsx from 'clsx';
import React, { FunctionComponent, useEffect, useState } from 'react';
import { Box, styled } from '@mui/material';
import { red } from '@mui/material/colors';
import { UploadCloud01, XCircle } from '@untitled-ui/icons-react';

import { FileUploadFixtures } from 'tg.fixtures/FileUploadFixtures';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

import { MAX_FILE_COUNT } from './Screenshots';

export interface ScreenshotDropzoneProps {
  validateAndUpload: (files: File[]) => void;
}

const StyledDropZoneValidation = styled(Box)`
  pointer-events: none;
  background-color: ${({ theme }) =>
    theme.palette.tokens._components.dropzone.active};
  border-radius: 4px;
  border: 1px dashed ${({ theme }) => theme.palette.secondary.main};
  backdrop-filter: blur(40px);
  display: flex;
  justify-content: center;
  align-items: center;

  &.invalid {
    border-color: ${red[200]};
    background-color: ${red[50]};
    backdrop-filter: blur(40px);
  }
`;

const StyledIconWrapper = styled('div')`
  display: flex;
  width: 64px;
  height: 64px;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: ${({ theme }) => theme.palette.tokens.background.onDefault};
  border-radius: 50%;
`;

const StyledValidIcon = styled(UploadCloud01)`
  width: 32px;
  height: 32px;
  color: ${({ theme }) => theme.palette.secondary.main};
`;

const StyledInvalidIcon = styled(XCircle)`
  width: 32px;
  height: 32px;
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
            invalid: dragOver === 'invalid',
          })}
          display="flex"
          alignItems="center"
          justifyContent="center"
        >
          {dragOver === 'valid' && (
            <StyledIconWrapper>
              <StyledValidIcon />
            </StyledIconWrapper>
          )}
          {dragOver === 'invalid' && (
            <StyledIconWrapper>
              <StyledInvalidIcon />
            </StyledIconWrapper>
          )}
        </StyledDropZoneValidation>
        {props.children}
      </Box>
    </>
  );
};
