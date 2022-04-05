import clsx from 'clsx';
import { Box, styled } from '@mui/material';
import { green, red } from '@mui/material/colors';
import { Backup, HighlightOff } from '@mui/icons-material';
import React, { FunctionComponent, useState } from 'react';

import { FileUploadFixtures } from 'tg.fixtures/FileUploadFixtures';

import { MAX_FILE_COUNT } from './ImportFileInput';

export interface ScreenshotDropzoneProps {
  onNewFiles: (files: File[]) => void;
}

const StyledWrapper = styled(Box)`
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.2s;

  &.valid {
    backdrop-filter: blur(5px);
    border: 1px solid ${green[200]};
    background-color: ${green[50]};
    opacity: 0.9;
  }

  &.invalid {
    border: 1px solid ${red[200]};
    opacity: 0.9;
    background-color: ${red[50]};
    backdrop-filter: blur(5px);
  }
`;

const StyledValidIcon = styled(Backup)`
  filter: drop-shadow(1px 1px 0px ${green[200]})
    drop-shadow(-1px 1px 0px ${green[200]})
    drop-shadow(1px -1px 0px ${green[200]})
    drop-shadow(-1px -1px 0px ${green[200]});
  font-size: 100px;
  color: ${({ theme }) => theme.palette.common.white};
`;

const StyledInvalidIcon = styled(HighlightOff)`
  filter: drop-shadow(1px 1px 0px ${red[200]})
    drop-shadow(-1px 1px 0px ${red[200]}) drop-shadow(1px -1px 0px ${red[200]})
    drop-shadow(-1px -1px 0px ${red[200]});
  font-size: 100px;
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
    e.stopPropagation();
    e.preventDefault();
    if (e.target === dragEnterTarget) {
      setDragOver(null);
    }
  };

  const onDrop = async (e: React.DragEvent) => {
    e.stopPropagation();
    e.preventDefault();
    if (e.dataTransfer.items) {
      const files = FileUploadFixtures.dataTransferItemsToArray(
        e.dataTransfer.items
      );
      props.onNewFiles(files);
    }
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
