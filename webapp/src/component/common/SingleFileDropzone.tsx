import React from 'react';
import { FileDropzone, FileDropzoneProps } from './FileDropzone';
import { FilesType } from 'tg.fixtures/FileUploadFixtures';

export type SingleFileDropzoneProps = Omit<
  FileDropzoneProps,
  'files' | 'onFilesSelect' | 'maxFiles'
> & {
  file: FilesType[number] | null;
  onFileSelect: (file: FilesType[number] | null) => void;
};

export const SingleFileDropzone: React.FC<SingleFileDropzoneProps> = ({
  file,
  onFileSelect,
  ...otherProps
}) => {
  const files = file ? [file] : [];

  const handleFilesSelect = (newFiles: FilesType) => {
    onFileSelect(newFiles.length > 0 ? newFiles[0] : null);
  };

  return (
    <FileDropzone
      files={files}
      onFilesSelect={handleFilesSelect}
      maxFiles={1}
      {...otherProps}
    />
  );
};
