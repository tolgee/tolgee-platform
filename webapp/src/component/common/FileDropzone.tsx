import React, { useRef, ReactNode } from 'react';
import { Box, Button, Typography, styled, Link } from '@mui/material';
import { T } from '@tolgee/react';
import { File02 } from '@untitled-ui/icons-react';

import { messageService } from 'tg.service/MessageService';
import { DragDropArea } from './DragDropArea';
import { FilesType } from 'tg.fixtures/FileUploadFixtures';
import { FileDropzoneSelectedFile } from 'tg.component/common/FileDropzoneSelectedFile';
import { useOnFilePaste } from 'tg.fixtures/useOnFilePaste';

const StyledContainer = styled(Box)`
  border: 2px dashed ${({ theme }) => theme.palette.tokens.border.secondary};
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  padding: ${({ theme }) => theme.spacing(6, 4)};
  text-align: center;
  background-color: ${({ theme }) =>
    theme.palette.tokens.background['paper-3']};
  transition: all 0.2s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(2)};
  margin-bottom: ${({ theme }) => theme.spacing(3)};
`;

const StyledIconWrapper = styled(Box)`
  width: 32px;
  height: 32px;
  color: ${({ theme }) => theme.palette.text.secondary};
  display: flex;
  align-items: center;
  justify-content: center;

  svg {
    width: 100%;
    height: 100%;
  }
`;

const StyledSupportedFilesIconsContainer = styled(Box)`
  display: flex;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.spacing(1)};
  justify-content: center;
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
  helpLink?: {
    href: string;
    text: ReactNode;
  };
};

export const FileDropzone: React.FC<FileDropzoneProps> = ({
  files,
  onFilesSelect,
  maxFiles = Infinity,
  acceptedFileTypes,
  helpLink,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const findFileType = (fileName: string) =>
    acceptedFileTypes.find((config) =>
      fileName.toLowerCase().endsWith(config.extension.toLowerCase())
    );

  const isValidFileType = (fileName: string) => {
    return findFileType(fileName) !== undefined;
  };

  const handleFilesReceived = (receivedFiles: FilesType) => {
    const validFiles = receivedFiles.filter((receivedFile) => {
      if (!isValidFileType(receivedFile.name)) {
        messageService.error(<T keyName="error_message_invalid_file_type" />);
        return false;
      }
      return true;
    });

    if (validFiles.length > 0) {
      const allFiles = [...files, ...validFiles];
      const newFiles = allFiles.slice(0, maxFiles);
      if (newFiles.length !== allFiles.length) {
        messageService.error(
          <T keyName="error_message_too_many_files" params={{ maxFiles }} />
        );
      }
      onFilesSelect(newFiles);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = e.target.files;
    if (selectedFiles && selectedFiles.length > 0) {
      const fileArray = Array.from(selectedFiles);
      handleFilesReceived(fileArray.map((f) => ({ file: f, name: f.name })));

      // reset the input value to allow selecting the same file again
      // since we can't sync the 'files' prop with the input value directly
      e.currentTarget.value = '';
    }
  };

  const handleRemoveFile = (indexToRemove: number) => {
    const newFiles = files.filter((_, index) => index !== indexToRemove);
    onFilesSelect(newFiles);

    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  useOnFilePaste(handleFilesReceived);

  const canAddMoreFiles = files.length < maxFiles;

  const handleClick = () => {
    if (canAddMoreFiles) {
      fileInputRef.current?.click();
    }
  };

  const renderFile = (file: FilesType[number], index: number) => {
    const FileIcon = findFileType(file.name)?.icon || File02;
    return (
      <FileDropzoneSelectedFile
        icon={<FileIcon />}
        file={file}
        key={`${file.name}-${index}`}
        onRemove={() => handleRemoveFile(index)}
      />
    );
  };

  const renderIcon = (config: FileType) => {
    const FileIcon = config.icon;
    if (FileIcon === undefined) {
      return null;
    }
    return (
      <StyledIconWrapper key={config.extension}>
        <FileIcon />
      </StyledIconWrapper>
    );
  };

  return (
    <>
      <input
        ref={fileInputRef}
        type="file"
        accept={acceptedFileTypes.map((config) => config.extension).join(',')}
        multiple={maxFiles > 1}
        style={{ display: 'none' }}
        onChange={handleFileChange}
      />

      <DragDropArea
        onFilesReceived={handleFilesReceived}
        onClick={handleClick}
        maxItems={maxFiles - files.length}
      >
        <StyledContainer>
          {files.length === 0 ? (
            <>
              <StyledSupportedFilesIconsContainer>
                {acceptedFileTypes.map(renderIcon)}
              </StyledSupportedFilesIconsContainer>
              <Typography variant="body1" color="text.primary">
                <T keyName="upload_drop_file_text" />
              </Typography>
              <Button
                variant="outlined"
                color="primary"
                onClick={(e) => {
                  e.stopPropagation();
                  fileInputRef.current?.click();
                }}
                data-cy="file-dropzone-select-button"
              >
                <T keyName="upload_select_file_button" />
              </Button>
            </>
          ) : (
            <Box width="100%">
              <Typography variant="subtitle2" mb={2}>
                <T keyName="upload_file_to_import_label" />
              </Typography>
              <Box display="flex" flexDirection="column" gap={1}>
                {files.map(renderFile)}
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
                  data-cy="file-dropzone-add-more-button"
                >
                  <T keyName="upload_add_more_files" />
                </Button>
              )}
            </Box>
          )}
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
        </StyledContainer>
      </DragDropArea>
    </>
  );
};
