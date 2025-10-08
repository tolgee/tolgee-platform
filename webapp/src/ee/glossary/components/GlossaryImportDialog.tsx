import React, { useState, useRef } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  styled,
  Radio,
  FormControlLabel,
  FormControl,
  IconButton,
  Link,
} from '@mui/material';
import { T } from '@tolgee/react';
import { XClose, Upload01 } from '@untitled-ui/icons-react';

import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';
import LoadingButton from 'tg.component/common/form/LoadingButton';

const StyledDropzone = styled(Box)<{ isDragActive?: boolean }>`
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
    border-color: ${({ theme }) => theme.palette.primary.main};
    background-color: ${({ theme }) => theme.palette.action.hover};
  }
`;

const StyledFileChip = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  padding: ${({ theme }) => theme.spacing(1, 2)};
  background-color: ${({ theme }) => theme.palette.background.paper};
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  border: 1px solid ${({ theme }) => theme.palette.divider};
`;

const StyledRadioOption = styled(Box)<{ selected?: boolean }>`
  border: 2px solid
    ${({ theme, selected }) =>
      selected ? theme.palette.primary.main : theme.palette.divider};
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  padding: ${({ theme }) => theme.spacing(2)};
  flex: 1;
  transition: all 0.2s ease;

  &:hover {
    border-color: ${({ theme }) => theme.palette.primary.main};
  }
`;

const StyledUploadIcon = styled(Upload01)`
  width: 32px;
  height: 32px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
  hasExistingTerms?: boolean;
};

export const GlossaryImportDialog: React.VFC<Props> = ({
  open,
  onClose,
  onFinished,
  hasExistingTerms = true,
}) => {
  const { preferredOrganization } = usePreferredOrganization();
  const glossary = useGlossary();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [file, setFile] = useState<File | null>(null);
  const [isDragActive, setIsDragActive] = useState(false);
  const [importMode, setImportMode] = useState<'add' | 'replace'>('add');

  const importMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/import',
    method: 'post',
    invalidatePrefix:
      '/v2/organizations/{organizationId}/glossaries/{glossaryId}',
  });

  const handleDragEnter = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragActive(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragActive(false);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragActive(false);

    const droppedFiles = e.dataTransfer.files;
    if (droppedFiles.length > 0) {
      const droppedFile = droppedFiles[0];
      if (droppedFile.name.endsWith('.csv')) {
        setFile(droppedFile);
      } else {
        messageService.error(<T keyName="glossary_import_invalid_file_type" />);
      }
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = e.target.files;
    if (selectedFiles && selectedFiles.length > 0) {
      setFile(selectedFiles[0]);
    }
  };

  const handleRemoveFile = () => {
    setFile(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleImport = async () => {
    if (!file) return;

    importMutation.mutate(
      {
        path: {
          organizationId: preferredOrganization!.id,
          glossaryId: glossary.id,
        },
        content: {
          'multipart/form-data': {
            file: file as any,
          },
        },
        query: {
          override_existing_terms: importMode === 'replace',
        },
      },
      {
        onSuccess() {
          messageService.success(
            <T keyName="glossary_import_success_message" />
          );
          onFinished();
          handleClose();
        },
        onError(error) {
          messageService.error(
            error?.message || <T keyName="glossary_import_error_message" />
          );
        },
      }
    );
  };

  const handleClose = () => {
    setFile(null);
    setImportMode('add');
    setIsDragActive(false);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
    onClose();
  };

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      maxWidth="sm"
      fullWidth
      data-cy="glossary-import-dialog"
    >
      <DialogTitle>
        <T keyName="glossary_import_title" />
      </DialogTitle>

      <DialogContent>
        <input
          ref={fileInputRef}
          type="file"
          accept=".csv"
          style={{ display: 'none' }}
          onChange={handleFileSelect}
        />

        <StyledDropzone
          isDragActive={isDragActive}
          onDragEnter={handleDragEnter}
          onDragLeave={handleDragLeave}
          onDragOver={handleDragOver}
          onDrop={handleDrop}
          onClick={() => !file && fileInputRef.current?.click()}
          data-cy="glossary-import-dropzone"
        >
          {!file ? (
            <>
              <StyledUploadIcon />
              <Typography variant="body1" color="text.primary">
                <T keyName="glossary_import_drop_file_text" />
              </Typography>
              <Button
                variant="outlined"
                color="primary"
                onClick={(e) => {
                  e.stopPropagation();
                  fileInputRef.current?.click();
                }}
                data-cy="glossary-import-select-file-button"
              >
                <T keyName="glossary_import_select_file_button" />
              </Button>
              <Link
                href="https://docs.tolgee.io/platform/projects_and_organizations/glossary/import/csv-format"
                target="_blank"
                rel="noopener"
                onClick={(e) => e.stopPropagation()}
              >
                <Typography variant="body2" color="primary">
                  <T keyName="glossary_import_csv_formatting_guide" />
                </Typography>
              </Link>
            </>
          ) : (
            <Box width="100%">
              <Typography variant="subtitle2" mb={2}>
                <T keyName="glossary_import_file_to_import_label" />
              </Typography>
              <StyledFileChip>
                <Typography variant="body2" sx={{ flex: 1 }}>
                  {file.name}
                </Typography>
                <IconButton
                  size="small"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleRemoveFile();
                  }}
                  data-cy="glossary-import-remove-file-button"
                >
                  <XClose style={{ width: 20, height: 20 }} />
                </IconButton>
              </StyledFileChip>
              <Link
                href="https://docs.tolgee.io/platform/projects_and_organizations/glossary/import/csv-format"
                target="_blank"
                rel="noopener"
                onClick={(e) => e.stopPropagation()}
                sx={{ mt: 2, display: 'inline-block' }}
              >
                <Typography variant="body2" color="primary">
                  <T keyName="glossary_import_csv_formatting_guide" />
                </Typography>
              </Link>
            </Box>
          )}
        </StyledDropzone>

        {file && hasExistingTerms && (
          <FormControl component="fieldset" fullWidth>
            <Box display="flex" gap={2} mb={2}>
              <StyledRadioOption selected={importMode === 'replace'}>
                <FormControlLabel
                  value="replace"
                  control={
                    <Radio
                      checked={importMode === 'replace'}
                      onChange={() => setImportMode('replace')}
                      data-cy="glossary-import-mode-replace"
                    />
                  }
                  label={
                    <Box>
                      <Typography variant="body1" fontWeight={500}>
                        <T keyName="glossary_import_mode_replace_title" />
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        <T keyName="glossary_import_mode_replace_description" />
                      </Typography>
                    </Box>
                  }
                />
              </StyledRadioOption>

              <StyledRadioOption selected={importMode === 'add'}>
                <FormControlLabel
                  value="add"
                  control={
                    <Radio
                      checked={importMode === 'add'}
                      onChange={() => setImportMode('add')}
                      data-cy="glossary-import-mode-add"
                    />
                  }
                  label={
                    <Box>
                      <Typography variant="body1" fontWeight={500}>
                        <T keyName="glossary_import_mode_add_title" />
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        <T keyName="glossary_import_mode_add_description" />
                      </Typography>
                    </Box>
                  }
                />
              </StyledRadioOption>
            </Box>
          </FormControl>
        )}
      </DialogContent>

      <DialogActions>
        <Button onClick={handleClose} data-cy="glossary-import-cancel-button">
          <T keyName="global_form_cancel" />
        </Button>
        <LoadingButton
          onClick={handleImport}
          variant="contained"
          color="primary"
          disabled={!file}
          loading={importMutation.isLoading}
          data-cy="glossary-import-submit-button"
        >
          <T keyName="glossary_import_submit_button" />
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};
