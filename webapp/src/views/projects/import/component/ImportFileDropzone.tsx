import clsx from 'clsx';
import { Box, styled } from '@mui/material';
import { green, red } from '@mui/material/colors';
import { XCircle } from '@untitled-ui/icons-react';
import React, { FunctionComponent, useState } from 'react';

import {
  FilesType,
  FileUploadFixtures,
  getFilesAsync,
} from 'tg.fixtures/FileUploadFixtures';

import { MAX_FILE_COUNT } from './ImportFileInput';
import { Dropzone } from 'tg.component/CustomIcons';

export interface ScreenshotDropzoneProps {
  onNewFiles: (files: FilesType) => void;
  active: boolean;
}

const StyledWrapper = styled(Box)`
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.2s;
  background-color: ${({ theme }) =>
    theme.palette.tokens.background['paper-1']};

  &:before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    border-radius: 4px;
    height: 100%;
  }

  &.valid,
  &.invalid {
    opacity: 1;
  }

  &.valid:before,
  &.invalid:before {
    backdrop-filter: blur(5px);
    opacity: 0.3;
  }

  &.valid:before {
    background-color: ${green[200]};
  }

  &.invalid:before {
    background-color: ${red[200]};
  }
`;

const StyledValidIcon = styled(Dropzone)`
  width: 100px;
  height: 100px;
  color: ${({ theme }) => theme.palette.import.progressDone};
`;

const StyledInvalidIcon = styled(XCircle)`
  width: 100px;
  height: 100px;
  fill: ${({ theme }) => theme.palette.error.main};
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
          {dragOver === 'valid' && <StyledValidIcon />}
          {dragOver === 'invalid' && <StyledInvalidIcon />}
        </StyledWrapper>
        {props.children}
      </Box>
    </>
  );
};
