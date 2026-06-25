import { useRef, useState } from 'react';
import { Alert, Box, styled, Typography } from '@mui/material';
import { File02 } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { DangerButton } from 'tg.component/DangerZone/DangerButton';
import { LabelHint } from 'tg.component/common/LabelHint';
import { SingleFileDropzone } from 'tg.component/common/SingleFileDropzone';
import { FilesType } from 'tg.fixtures/FileUploadFixtures';
import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useConfig } from 'tg.globalContext/helpers';
import { confirmation } from 'tg.hooks/confirmation';
import { messageService } from 'tg.service/MessageService';
import { downloadResponseAsFile } from 'tg.fixtures/downloadResponseAsFile';

import {
  ExportManifest,
  readExportManifest,
} from './components/exportImport/readExportManifest';

const StyledManifest = styled(Box)`
  display: grid;
  gap: 4px;
  padding: 12px 16px;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-radius: 6px;
`;

const StyledManifestRow = styled(Box)`
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 8px;
`;

export const ProjectSettingsExportImport = () => {
  const project = useProject();
  const config = useConfig();

  const [file, setFile] = useState<FilesType[number] | null>(null);
  const [manifest, setManifest] = useState<ExportManifest | null>(null);
  const [manifestUnreadable, setManifestUnreadable] = useState(false);
  const [manifestLoading, setManifestLoading] = useState(false);
  // Guards against a slow manifest read for an earlier selection resolving
  // after a newer file was picked and overwriting the current preview.
  const manifestRequestId = useRef(0);

  const exportLoadable = useApiMutation({
    url: '/v2/administration/projects/{projectId}/export',
    method: 'get',
    fetchOptions: { rawResponse: true },
  });

  const importLoadable = useApiMutation({
    url: '/v2/administration/projects/{projectId}/import',
    method: 'post',
    invalidatePrefix: '/v2/projects',
  });

  const handleExport = () => {
    exportLoadable.mutate(
      { path: { projectId: project.id } },
      {
        async onSuccess(response) {
          await downloadResponseAsFile(
            response as unknown as Response,
            `${project.name}.zip`
          );
        },
      }
    );
  };

  const clearSelection = () => {
    manifestRequestId.current += 1;
    setFile(null);
    setManifest(null);
    setManifestUnreadable(false);
    setManifestLoading(false);
  };

  const handleFileSelect = (selected: FilesType[number] | null) => {
    setFile(selected);
    setManifest(null);
    setManifestUnreadable(false);
    const requestId = (manifestRequestId.current += 1);
    if (!selected) {
      setManifestLoading(false);
      return;
    }
    setManifestLoading(true);
    readExportManifest(selected.file)
      .then((result) => {
        if (manifestRequestId.current === requestId) {
          setManifest(result);
          setManifestLoading(false);
        }
      })
      .catch(() => {
        if (manifestRequestId.current === requestId) {
          setManifestUnreadable(true);
          setManifestLoading(false);
        }
      });
  };

  const versionMismatch =
    manifest !== null && manifest.schemaVersion !== config.version;

  const handleImport = () => {
    const selected = file;
    if (!selected || manifestLoading || manifestUnreadable || versionMismatch) {
      return;
    }
    confirmation({
      title: (
        <T
          keyName="project_settings_import_confirm_title"
          defaultValue="Replace project content"
        />
      ),
      message: (
        <T
          keyName="project_settings_import_confirm_message"
          defaultValue="This permanently deletes all content of <b>{name}</b> and replaces it with the uploaded archive. This cannot be undone."
          params={{ name: project.name, b: <b /> }}
        />
      ),
      confirmButtonText: (
        <T
          keyName="project_settings_import_confirm_button"
          defaultValue="Import & replace"
        />
      ),
      hardModeText: project.name.toUpperCase(),
      onConfirm() {
        importLoadable.mutate(
          {
            path: { projectId: project.id },
            content: {
              'multipart/form-data': { file: selected.file as any },
            },
          },
          {
            onSuccess() {
              messageService.success(
                <T
                  keyName="project_settings_import_success_message"
                  defaultValue="Project imported successfully."
                />
              );
              clearSelection();
            },
          }
        );
      },
    });
  };

  const entityCounts = Object.entries(manifest?.entityCounts ?? {})
    .filter(([, count]) => count > 0)
    .map(([type, count]) => `${type}: ${count}`)
    .join(', ');

  return (
    <Box display="grid" mb={8} data-cy="project-settings-export-import">
      <Typography variant="h5" mt={4} mb="20px">
        <T
          keyName="project_settings_export_title"
          defaultValue="Export project"
        />
      </Typography>
      <Typography variant="body1" mb={2}>
        <T
          keyName="project_settings_export_description"
          defaultValue="Download this project as a self-contained zip archive. It can be imported onto a project on another Tolgee instance running the same version."
        />
      </Typography>
      <Box>
        <LoadingButton
          data-cy="project-settings-export-button"
          loading={exportLoadable.isLoading}
          variant="contained"
          color="primary"
          onClick={handleExport}
        >
          <T
            keyName="project_settings_export_button"
            defaultValue="Export project"
          />
        </LoadingButton>
      </Box>

      <Typography variant="h5" mt={5} mb="20px">
        <T
          keyName="project_settings_import_title"
          defaultValue="Import project"
        />
      </Typography>
      <Typography variant="body1" mb={2}>
        <T
          keyName="project_settings_import_description"
          defaultValue="Upload a project export archive to replace this project's content. <b>This permanently deletes all current content of this project</b> and replaces it with the uploaded archive."
          params={{ b: <b /> }}
        />
      </Typography>

      <LabelHint
        title={
          <T
            keyName="project_settings_import_user_mapping_hint"
            defaultValue="Users are matched by username and email. Anything authored by a user not present on this instance is attributed to you."
          />
        }
        sx={{ mb: 2 }}
      >
        <Typography variant="body2" color="textSecondary">
          <T
            keyName="project_settings_import_user_mapping_label"
            defaultValue="How are users mapped?"
          />
        </Typography>
      </LabelHint>

      <SingleFileDropzone
        file={file}
        onFileSelect={handleFileSelect}
        acceptedFileTypes={[{ extension: '.zip', icon: File02 }]}
      />

      {manifestUnreadable && (
        <Alert severity="error" sx={{ mb: 2 }}>
          <T
            keyName="project_settings_import_manifest_unreadable"
            defaultValue="Could not read the export archive. Make sure you selected a valid Tolgee project export zip."
          />
        </Alert>
      )}

      {manifest && (
        <StyledManifest mb={2} data-cy="project-settings-import-manifest">
          <StyledManifestRow>
            <Typography variant="body2" color="textSecondary">
              <T
                keyName="project_settings_import_manifest_source_project"
                defaultValue="Source project"
              />
            </Typography>
            <Typography variant="body2">
              {manifest.sourceProjectName}
            </Typography>
          </StyledManifestRow>
          <StyledManifestRow>
            <Typography variant="body2" color="textSecondary">
              <T
                keyName="project_settings_import_manifest_version"
                defaultValue="Tolgee version"
              />
            </Typography>
            <Typography variant="body2">{manifest.schemaVersion}</Typography>
          </StyledManifestRow>
          <StyledManifestRow>
            <Typography variant="body2" color="textSecondary">
              <T
                keyName="project_settings_import_manifest_content"
                defaultValue="Content"
              />
            </Typography>
            <Typography variant="body2">{entityCounts}</Typography>
          </StyledManifestRow>
        </StyledManifest>
      )}

      {manifest && versionMismatch && (
        <Alert
          severity="warning"
          sx={{ mb: 2 }}
          data-cy="project-settings-import-version-warning"
        >
          <T
            keyName="project_settings_import_version_warning"
            defaultValue="This archive was exported from Tolgee {manifestVersion}, but this instance runs {runningVersion}. Import is only supported between matching versions."
            params={{
              manifestVersion: manifest.schemaVersion,
              runningVersion: config.version,
            }}
          />
        </Alert>
      )}

      <Box>
        <DangerButton
          data-cy="project-settings-import-button"
          loading={importLoadable.isLoading}
          disabled={
            !file || manifestLoading || manifestUnreadable || versionMismatch
          }
          onClick={handleImport}
        >
          <T
            keyName="project_settings_import_button"
            defaultValue="Import & replace"
          />
        </DangerButton>
      </Box>
    </Box>
  );
};
