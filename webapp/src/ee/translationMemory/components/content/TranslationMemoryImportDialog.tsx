import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Alert,
  Box,
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
import { useTmxOversizeScan } from 'tg.ee.module/translationMemory/hooks/useTmxOversizeScan';
import { TM_ENTRY_TEXT_MAX_LENGTH } from 'tg.ee.module/translationMemory/services/scanTmxForOversize';

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
  const scan = useTmxOversizeScan(file?.file ?? null);

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
          const noChanges =
            result.created === 0 &&
            result.updated === 0 &&
            result.skipped === 0;
          messageService.success(
            noChanges ? (
              <T
                keyName="translation_memory_import_no_changes"
                defaultValue="No changes"
              />
            ) : (
              <T
                keyName="translation_memory_import_summary"
                defaultValue="{created} created, {updated} updated, {skipped} skipped"
                params={{
                  created: result.created,
                  updated: result.updated,
                  skipped: result.skipped,
                }}
              />
            )
          );
          onFinished();
        },
        onError: () =>
          messageService.error(
            <T
              keyName="translation_memory_import_error"
              defaultValue="Import failed"
            />
          ),
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
          helperText={
            <T
              keyName="translation_memory_import_tmx_format_note"
              defaultValue="Only TMX 1.4b files are supported."
            />
          }
        />

        <Box mt={2} mb={2}>
          {scan.kind === 'scanned' && scan.oversizeSegments > 0 && (
            <Alert severity="warning" data-cy="tm-import-oversize-warning">
              <T
                keyName="translation_memory_import_oversize_warning"
                defaultValue="Found {count, plural, one {# text} other {# texts}} over {limit} characters. {count, plural, one {This} other {These}} will be skipped unless changed."
                params={{
                  count: scan.oversizeSegments,
                  limit: TM_ENTRY_TEXT_MAX_LENGTH,
                }}
              />
            </Alert>
          )}

          {scan.kind === 'tooLargeToScan' && (
            <Alert severity="info" data-cy="tm-import-large-file-notice">
              <T
                keyName="translation_memory_import_large_file_notice"
                defaultValue="File too large to pre-check. Texts over {limit} characters will be skipped on import."
                params={{ limit: TM_ENTRY_TEXT_MAX_LENGTH }}
              />
            </Alert>
          )}
        </Box>

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
