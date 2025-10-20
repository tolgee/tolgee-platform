import { FilesType } from 'tg.fixtures/FileUploadFixtures';
import { Box, IconButton, styled, Typography } from '@mui/material';
import { XClose } from '@untitled-ui/icons-react';
import React, { ReactNode } from 'react';
import { useTranslate } from '@tolgee/react';

const StyledFileChip = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  padding: ${({ theme }) => theme.spacing(1, 2)};
  background-color: ${({ theme }) => theme.palette.background.paper};
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  border: 1px solid ${({ theme }) => theme.palette.divider};
`;

const StyledFileIcon = styled('div')`
  width: 20px;
  height: 20px;
  color: ${({ theme }) => theme.palette.text.secondary};
  display: flex;
  align-items: center;
  justify-content: center;

  svg {
    width: 100%;
    height: 100%;
  }
`;

export type FileDropzoneSelectedFileProps = {
  icon: ReactNode;
  file: FilesType[number];
  onRemove?: () => void;
};

export const FileDropzoneSelectedFile: React.FC<
  FileDropzoneSelectedFileProps
> = ({ icon, file, onRemove }) => {
  const { t } = useTranslate();
  return (
    <StyledFileChip>
      <StyledFileIcon>{icon}</StyledFileIcon>
      <Typography variant="body2" sx={{ flex: 1 }}>
        {file.name}
      </Typography>
      <IconButton
        size="small"
        onClick={(e) => {
          e.stopPropagation();
          onRemove?.();
        }}
        aria-label={t('file-dropzone-remove-file-button')}
        data-cy={`file-dropzone-remove-button`}
      >
        <XClose style={{ width: 20, height: 20 }} />
      </IconButton>
    </StyledFileChip>
  );
};
