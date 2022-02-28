import React, { FunctionComponent, useState } from 'react';
import { Box, createStyles, makeStyles, Theme } from '@material-ui/core';
import { green, red } from '@material-ui/core/colors';
import { Backup, HighlightOff } from '@material-ui/icons';
import clsx from 'clsx';

import { FileUploadFixtures } from 'tg.fixtures/FileUploadFixtures';

import { MAX_FILE_COUNT } from './ImportFileInput';

export interface ScreenshotDropzoneProps {
  onNewFiles: (files: File[]) => void;
}

const useStyles = makeStyles((theme: Theme) =>
  createStyles({
    dropZoneValidation: {
      pointerEvents: 'none',
      opacity: 0,
      transition: 'opacity 0.2s',
    },
    valid: {
      backdropFilter: 'blur(5px)',
      border: `1px solid ${green[200]}`,
      backgroundColor: green[50],
      opacity: 0.9,
    },
    invalid: {
      border: `1px solid ${red[200]}`,
      opacity: 0.9,
      backgroundColor: red[50],
      backdropFilter: 'blur(5px)',
    },
    validIcon: {
      filter: `drop-shadow(1px 1px 0px ${green[200]}) drop-shadow(-1px 1px 0px ${green[200]})
         drop-shadow(1px -1px 0px ${green[200]}) drop-shadow(-1px -1px 0px ${green[200]})`,
      fontSize: 100,
      color: theme.palette.common.white,
    },
    invalidIcon: {
      filter: `drop-shadow(1px 1px 0px ${red[200]}) drop-shadow(-1px 1px 0px ${red[200]})
         drop-shadow(1px -1px 0px ${red[200]}) drop-shadow(-1px -1px 0px ${red[200]})`,
      fontSize: 100,
      color: theme.palette.common.white,
    },
  })
);

export const ImportFileDropzone: FunctionComponent<ScreenshotDropzoneProps> = (
  props
) => {
  const [dragOver, setDragOver] = useState(null as null | 'valid' | 'invalid');
  const [dragEnterTarget, setDragEnterTarget] = useState(
    null as EventTarget | null
  );
  const classes = useStyles({});

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
        <Box
          data-cy="dropzone-inner"
          zIndex={2}
          position="absolute"
          width="100%"
          height="100%"
          className={clsx({
            [classes.dropZoneValidation]: true,
            [classes.valid]: dragOver === 'valid',
            [classes.invalid]: dragOver === 'invalid',
          })}
          display="flex"
          alignItems="center"
          justifyContent="center"
        >
          {dragOver === 'valid' && <Backup className={classes.validIcon} />}
          {dragOver === 'invalid' && (
            <HighlightOff className={classes.invalidIcon} />
          )}
        </Box>
        {props.children}
      </Box>
    </>
  );
};
