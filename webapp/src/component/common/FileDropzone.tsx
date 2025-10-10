import React, { useRef, ReactNode } from 'react';
import {
  Box,
  Button,
  Typography,
  styled,
  IconButton,
  Link,
} from '@mui/material';
import { T } from '@tolgee/react';
import { XClose, Upload01, File02 } from '@untitled-ui/icons-react';

import { messageService } from 'tg.service/MessageService';
import { DragDropArea } from './DragDropArea';
import { ImportFileDropzone } from 'tg.views/projects/import/component/ImportFileDropzone';
import { FilesType } from 'tg.fixtures/FileUploadFixtures';

const StyledFileChip = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  padding: ${({ theme }) => theme.spacing(1, 2)};
  background-color: ${({ theme }) => theme.palette.background.paper};
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  border: 1px solid ${({ theme }) => theme.palette.divider};
`;

const StyledUploadIcon = styled(Upload01)`
  width: 32px;
  height: 32px;
  color: ${({ theme }) => theme.palette.text.secondary};
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

export type FileType = {
  extension: string;
  icon: React.ComponentType<any>;
};

export type FileDropzoneProps = {
  files: FilesType;
  onFilesSelect: (files: FilesType) => void;
  maxFiles?: number;
  acceptedFileTypes: FileType[];
  dropzoneText: ReactNode;
  selectButtonText: ReactNode;
  helpLink?: {
    href: string;
    text: ReactNode;
  };
  invalidFileTypeMessage?: ReactNode;
  dataCyDropzone?: string;
  dataCySelectButton?: string;
  dataCyRemoveButton?: string;
};

export const FileDropzone: React.FC<FileDropzoneProps> = ({
  files,
  onFilesSelect,
  maxFiles = Infinity,
  acceptedFileTypes,
  dropzoneText,
  selectButtonText,
  helpLink,
  invalidFileTypeMessage,
  dataCyDropzone,
  dataCySelectButton,
  dataCyRemoveButton,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const getExtensions = () => {
    return acceptedFileTypes.map((config) => config.extension);
  };

  const isValidFileType = (fileName: string) => {
    return getExtensions().some((type) =>
      fileName.toLowerCase().endsWith(type.toLowerCase())
    );
  };

  const getFileIcon = (fileName: string) => {
    const fileConfig = acceptedFileTypes.find((config) =>
      fileName.toLowerCase().endsWith(config.extension.toLowerCase())
    );
    // Return the configured icon for the file type, or File02 as the default file icon fallback
    return fileConfig?.icon || File02;
  };

  const handleFilesReceived = (receivedFiles: FilesType) => {
    const validFiles = receivedFiles.filter((receivedFile) => {
      if (!isValidFileType(receivedFile.name)) {
        if (invalidFileTypeMessage) {
          messageService.error(invalidFileTypeMessage);
        }
        return false;
      }
      return true;
    });

    if (validFiles.length > 0) {
      const newFiles = [...files, ...validFiles].slice(0, maxFiles);
      onFilesSelect(newFiles);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = e.target.files;
    if (selectedFiles && selectedFiles.length > 0) {
      const fileArray = Array.from(selectedFiles);
      handleFilesReceived(fileArray.map((f) => ({ file: f, name: f.name })));
    }
  };

  const handleRemoveFile = (indexToRemove: number) => {
    const newFiles = files.filter((_, index) => index !== indexToRemove);
    onFilesSelect(newFiles);

    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleClick = () => {
    if (files.length < maxFiles) {
      fileInputRef.current?.click();
    }
  };

  const canAddMoreFiles = files.length < maxFiles;

  return (
    <>
      <input
        ref={fileInputRef}
        type="file"
        accept={getExtensions().join(',')}
        multiple={maxFiles > 1}
        style={{ display: 'none' }}
        onChange={handleFileChange}
      />

      <DragDropArea
        onFilesReceived={handleFilesReceived}
        onClick={handleClick}
        data-cy={dataCyDropzone}
      >
        {files.length === 0 ? (
          <>
            <StyledUploadIcon />
            <Typography variant="body1" color="text.primary">
              {dropzoneText}
            </Typography>
            <Button
              variant="outlined"
              color="primary"
              onClick={(e) => {
                e.stopPropagation();
                fileInputRef.current?.click();
              }}
              data-cy={dataCySelectButton}
            >
              {selectButtonText}
            </Button>
            {helpLink && (
              <Link
                href={helpLink.href}
                target="_blank"
                rel="noopener"
                onClick={(e) => e.stopPropagation()}
              >
                <Typography variant="body2" color="primary">
                  {helpLink.text}
                </Typography>
              </Link>
            )}
          </>
        ) : (
          <Box width="100%">
            <Typography variant="subtitle2" mb={2}>
              <T keyName="glossary_import_file_to_import_label" />
            </Typography>
            <Box display="flex" flexDirection="column" gap={1}>
              {files.map((selectedFile, index) => {
                const FileIconComponent = getFileIcon(selectedFile.name);
                return (
                  <StyledFileChip key={`${selectedFile.name}-${index}`}>
                    <StyledFileIcon>
                      <FileIconComponent />
                    </StyledFileIcon>
                    <Typography variant="body2" sx={{ flex: 1 }}>
                      {selectedFile.name}
                    </Typography>
                    <IconButton
                      size="small"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleRemoveFile(index);
                      }}
                      data-cy={`${dataCyRemoveButton}-${index}`}
                    >
                      <XClose style={{ width: 20, height: 20 }} />
                    </IconButton>
                  </StyledFileChip>
                );
              })}
            </Box>
            {canAddMoreFiles && (
              <Button
                variant="outlined"
                color="primary"
                onClick={(e) => {
                  e.stopPropagation();
                  fileInputRef.current?.click();
                }}
                sx={{ mt: 2 }}
                data-cy={`${dataCySelectButton}-add-more`}
              >
                <T keyName="add_more_files" />
              </Button>
            )}
            {helpLink && (
              <Link
                href={helpLink.href}
                target="_blank"
                rel="noopener"
                onClick={(e) => e.stopPropagation()}
                sx={{ mt: 2, display: 'inline-block' }}
              >
                <Typography variant="body2" color="primary">
                  {helpLink.text}
                </Typography>
              </Link>
            )}
          </Box>
        )}
      </DragDropArea>
    </>
  );
};
