import React, { useState, ReactNode } from 'react';
import { Box, styled } from '@mui/material';
import { UploadCloud01, XCircle } from '@untitled-ui/icons-react';
import { FilesType, getFilesAsync } from 'tg.fixtures/FileUploadFixtures';
import clsx from 'clsx';

const StyledOverlay = styled(Box)`
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.2s;
  background-color: ${({ theme }) =>
    theme.palette.tokens._components.dropzone.active};
  border-radius: 4px;
  border: 1px dashed ${({ theme }) => theme.palette.secondary.main};
  backdrop-filter: blur(40px);

  &.visible {
    opacity: 1;
  }

  &.invalid {
    border-color: ${({ theme }) =>
      theme.palette.tokens.error._states.outlinedBorder};
    background-color: ${({ theme }) =>
      theme.palette.tokens._components.alert.error.color};
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
  color: ${({ theme }) => theme.palette.text.primary};
`;

export type DragDropAreaProps = {
  onFilesReceived: (files: FilesType) => void;
  onClick?: () => void;
  active?: boolean;
  showOverlay?: boolean;
  maxItems?: number;
  children: ReactNode;
  'data-cy'?: string;
};

export const DragDropArea: React.FC<DragDropAreaProps> = ({
  onFilesReceived,
  onClick,
  active = true,
  showOverlay,
  maxItems,
  children,
  'data-cy': dataCy,
}) => {
  const [dragOver, setDragOver] = useState<null | 'valid' | 'invalid'>(null);
  const [dragEnterTarget, setDragEnterTarget] = useState<EventTarget | null>(
    null
  );

  const handleDragEnter = (e: React.DragEvent) => {
    if (!active) return;
    e.preventDefault();
    e.stopPropagation();
    setDragEnterTarget(e.target);

    const items = e.dataTransfer?.items;
    const invalidAmount =
      maxItems !== undefined && items && items.length > maxItems;
    if (invalidAmount) {
      setDragOver('invalid');
    } else {
      setDragOver('valid');
    }
  };

  const handleDragLeave = (e: React.DragEvent) => {
    if (!active) return;
    e.preventDefault();
    e.stopPropagation();
    if (e.target === dragEnterTarget) {
      setDragOver(null);
      setDragEnterTarget(null);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    if (!active) return;
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDrop = async (e: React.DragEvent) => {
    if (!active) return;
    e.preventDefault();
    e.stopPropagation();

    const files = await getFilesAsync(e.dataTransfer);
    onFilesReceived(files);
    setDragOver(null);
    setDragEnterTarget(null);
  };

  const canShow = showOverlay !== false;
  const forceShow = showOverlay === true;

  return (
    <Box
      position="relative"
      overflow="visible"
      onClick={onClick}
      data-cy={dataCy}
      onDragEnter={handleDragEnter}
      onDragLeave={handleDragLeave}
      onDragOver={handleDragOver}
      onDrop={handleDrop}
    >
      <StyledOverlay
        className={clsx({
          visible: (canShow && dragOver !== null) || forceShow,
          valid: dragOver === 'valid',
          invalid: dragOver === 'invalid',
        })}
        position="absolute"
        width="100%"
        height="100%"
        zIndex={2}
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
      </StyledOverlay>
      {children}
    </Box>
  );
};
