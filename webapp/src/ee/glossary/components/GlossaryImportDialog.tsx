import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
} from '@mui/material';
import { T } from '@tolgee/react';

import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { ModeSelector, ModeOption } from 'tg.component/common/ModeSelector';
import { SingleFileDropzone } from 'tg.component/common/SingleFileDropzone';
import CsvLogo from 'tg.svgs/logos/csv.svg?react';
import { FilesType } from 'tg.fixtures/FileUploadFixtures';

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
  hasExistingTerms?: boolean;
};

type ImportMode = 'replace' | 'add';

const IMPORT_MODE_OPTIONS: ModeOption<ImportMode>[] = [
  {
    value: 'replace',
    title: <T keyName="glossary_import_mode_replace_title" />,
    description: <T keyName="glossary_import_mode_replace_description" />,
    dataCy: 'glossary-import-mode-replace',
  },
  {
    value: 'add',
    title: <T keyName="glossary_import_mode_add_title" />,
    description: <T keyName="glossary_import_mode_add_description" />,
    dataCy: 'glossary-import-mode-add',
  },
];

export const GlossaryImportDialog: React.VFC<Props> = ({
  open,
  onClose,
  onFinished,
  hasExistingTerms = true,
}) => {
  const glossary = useGlossary();

  const [file, setFile] = useState<FilesType[number] | null>(null);
  const [importMode, setImportMode] = useState<ImportMode>('replace');

  const importMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/import',
    method: 'post',
    invalidatePrefix:
      '/v2/organizations/{organizationId}/glossaries/{glossaryId}',
  });

  const handleImport = async () => {
    if (!file) return;

    importMutation.mutate(
      {
        path: {
          organizationId: glossary.organizationOwner.id,
          glossaryId: glossary.id,
        },
        content: {
          'multipart/form-data': {
            file: file.file as any,
          },
        },
        query: {
          removeExistingTerms: importMode === 'replace',
        },
      },
      {
        onSuccess() {
          messageService.success(
            <T keyName="glossary_import_success_message" />
          );
          onFinished();
        },
      }
    );
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      data-cy="glossary-import-dialog"
    >
      <DialogTitle>
        <T keyName="glossary_import_title" />
      </DialogTitle>

      <DialogContent>
        <SingleFileDropzone
          file={file}
          onFileSelect={setFile}
          acceptedFileTypes={[{ extension: '.csv', icon: CsvLogo }]}
          helpLink={{
            href: 'https://docs.tolgee.io/platform/projects_and_organizations/managing_glossaries#importing-terms-to-a-glossary',
            text: <T keyName="glossary_import_csv_formatting_guide" />,
          }}
        />

        {file && hasExistingTerms && (
          <ModeSelector
            value={importMode}
            onChange={setImportMode}
            options={IMPORT_MODE_OPTIONS}
          />
        )}
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose} data-cy="glossary-import-cancel-button">
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
