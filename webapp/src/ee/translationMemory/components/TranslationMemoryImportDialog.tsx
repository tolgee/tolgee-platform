import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
} from '@mui/material';
import { T } from '@tolgee/react';

import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { ModeSelector, ModeOption } from 'tg.component/common/ModeSelector';
import { SingleFileDropzone } from 'tg.component/common/SingleFileDropzone';
import { FilesType } from 'tg.fixtures/FileUploadFixtures';
import { File02 } from '@untitled-ui/icons-react';

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
  organizationId: number;
  translationMemoryId: number;
  hasExistingEntries: boolean;
};

type ImportMode = 'keep' | 'override';

const IMPORT_MODE_OPTIONS: ModeOption<ImportMode>[] = [
  {
    value: 'keep',
    title: (
      <T
        keyName="translation_memory_import_mode_keep_title"
        defaultValue="Keep existing entries"
      />
    ),
    description: (
      <T
        keyName="translation_memory_import_mode_keep_description"
        defaultValue="New entries are always added. Previously imported entries are kept unchanged."
      />
    ),
    dataCy: 'tm-import-mode-keep',
  },
  {
    value: 'override',
    title: (
      <T
        keyName="translation_memory_import_mode_override_title"
        defaultValue="Override existing entries"
      />
    ),
    description: (
      <T
        keyName="translation_memory_import_mode_override_description"
        defaultValue="New entries are always added. Previously imported entries are replaced with new translations."
      />
    ),
    dataCy: 'tm-import-mode-override',
  },
];

export const TranslationMemoryImportDialog: React.VFC<Props> = ({
  open,
  onClose,
  onFinished,
  organizationId,
  translationMemoryId,
  hasExistingEntries,
}) => {
  const [file, setFile] = useState<FilesType[number] | null>(null);
  const [importMode, setImportMode] = useState<ImportMode>('keep');

  const importMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/import',
    method: 'post',
    invalidatePrefix:
      '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}',
  });

  const handleImport = async () => {
    if (!file) return;

    importMutation.mutate(
      {
        path: { organizationId, translationMemoryId },
        content: {
          'multipart/form-data': {
            file: file.file as any,
          },
        },
        query: {
          overrideExisting: importMode === 'override',
        },
      },
      {
        onSuccess(data) {
          const result = data as components['schemas']['TmxImportResult'];
          const parts: string[] = [];
          if (result.created > 0) parts.push(`${result.created} created`);
          if (result.updated > 0) parts.push(`${result.updated} updated`);
          if (result.skipped > 0) parts.push(`${result.skipped} skipped`);
          const summary = parts.length > 0 ? parts.join(', ') : 'No changes';
          messageService.success(summary);
          onFinished();
        },
        onError: () => messageService.error('Import failed'),
      }
    );
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      data-cy="tm-import-dialog"
    >
      <DialogTitle>
        <T
          keyName="translation_memory_import_title"
          defaultValue="Import TMX file"
        />
      </DialogTitle>

      <DialogContent>
        <SingleFileDropzone
          file={file}
          onFileSelect={setFile}
          acceptedFileTypes={[{ extension: '.tmx', icon: File02 }]}
        />
        <Typography
          variant="caption"
          color="text.secondary"
          display="block"
          mt={1}
        >
          <T
            keyName="translation_memory_import_tmx_format_note"
            defaultValue="Only TMX 1.4b files are supported."
          />
        </Typography>

        {file && hasExistingEntries && (
          <ModeSelector
            value={importMode}
            onChange={setImportMode}
            options={IMPORT_MODE_OPTIONS}
          />
        )}
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose} data-cy="tm-import-cancel">
          <T keyName="global_form_cancel" defaultValue="Cancel" />
        </Button>
        <LoadingButton
          onClick={handleImport}
          variant="contained"
          color="primary"
          disabled={!file}
          loading={importMutation.isLoading}
          data-cy="tm-import-submit"
        >
          <T keyName="translation_memory_import_submit" defaultValue="Import" />
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};
