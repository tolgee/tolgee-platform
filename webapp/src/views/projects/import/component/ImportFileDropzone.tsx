import clsx from 'clsx';
import { Box, styled } from '@mui/material';
import { red } from '@mui/material/colors';
import { UploadCloud01, XCircle } from '@untitled-ui/icons-react';
import React, { FunctionComponent, useState } from 'react';

import {
  FilesType,
  FileUploadFixtures,
  getFilesAsync,
} from 'tg.fixtures/FileUploadFixtures';

import { MAX_FILE_COUNT } from './ImportFileInput';

export interface ScreenshotDropzoneProps {
  onNewFiles: (files: FilesType) => void;
  active: boolean;
}

const StyledWrapper = styled(Box)`
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.2s;
  background-color: ${({ theme }) =>
    theme.palette.tokens._components.dropzone.active};
  border-radius: 4px;
  border: 1px dashed ${({ theme }) => theme.palette.secondary.main};

  &.valid,
  &.invalid {
    opacity: 1;
  }

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

export const ImportFileDropzone: FunctionComponent<ScreenshotDropzoneProps> = (
  props
) => {
  const [dragOver, setDragOver] = useState(null as null | 'valid' | 'invalid');
  const [dragEnterTarget, setDragEnterTarget] = useState(
    null as EventTarget | null
  );

  const onDragEnter = (e: React.DragEvent) => {
    if (!props.active) {
      return;
    }
    e.stopPropagation();
    e.preventDefault();
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
    if (!props.active) {
      return;
    }
    e.stopPropagation();
    e.preventDefault();
    if (e.target === dragEnterTarget) {
      setDragOver(null);
    }
  };

  const onDrop = async (e: React.DragEvent) => {
    if (!props.active) {
      return;
    }
    e.stopPropagation();
    e.preventDefault();

    const files = await getFilesAsync(e.dataTransfer);
    props.onNewFiles(files);
    setDragOver(null);
  };

  return (
    <>
      <Box
        position="relative"
        onDrop={onDrop}
        onDragEnter={onDragEnter}
        onDragLeave={onDragLeave}
        overflow="visible"
        data-cy="dropzone"
      >
        <StyledWrapper
          data-cy="dropzone-inner"
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
        </StyledWrapper>
        {props.children}
      </Box>
    </>
  );
};
