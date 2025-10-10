import React, { useState, ReactNode } from 'react';
import { Box, styled } from '@mui/material';
import { FilesType, getFilesAsync } from 'tg.fixtures/FileUploadFixtures';

const StyledDragDropArea = styled(Box)<{ isDragActive?: boolean }>`
  border: 2px dashed
    ${({ theme, isDragActive }) =>
      isDragActive
        ? theme.palette.primary.main
        : theme.palette.tokens.border.secondary};
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  padding: ${({ theme }) => theme.spacing(6, 4)};
  text-align: center;
  background-color: ${({ theme, isDragActive }) =>
    isDragActive
      ? theme.palette.action.hover
      : theme.palette.tokens.background['paper-3']};
  transition: all 0.2s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(2)};
  margin-bottom: ${({ theme }) => theme.spacing(3)};

  &:hover {
    border-color: ${({ theme, isDragActive }) =>
      isDragActive
        ? theme.palette.primary.main
        : theme.palette.tokens.border.secondary};
    background-color: ${({ theme, isDragActive }) =>
      isDragActive
        ? theme.palette.action.hover
        : theme.palette.tokens.background['paper-3']};
  }
`;

export type DragDropAreaProps = {
  onFilesReceived: (files: FilesType) => void;
  onClick: () => void;
  children: ReactNode;
  'data-cy'?: string;
};

export const DragDropArea: React.FC<DragDropAreaProps> = ({
  onFilesReceived,
  onClick,
  children,
  'data-cy': dataCy,
}) => {
  const [dragEnterTarget, setDragEnterTarget] = useState(
    null as EventTarget | null
  );

  const handleDragEnter = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragEnterTarget(e.target);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.target === dragEnterTarget) {
      setDragEnterTarget(null);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();

    const files = await getFilesAsync(e.dataTransfer);
    onFilesReceived(files);
    setDragEnterTarget(null);
  };

  return (
    <StyledDragDropArea
      isDragActive={dragEnterTarget !== null}
      onDragEnter={handleDragEnter}
      onDragLeave={handleDragLeave}
      onDragOver={handleDragOver}
      onDrop={handleDrop}
      onClick={onClick}
      data-cy={dataCy}
    >
      {children}
    </StyledDragDropArea>
  );
};
