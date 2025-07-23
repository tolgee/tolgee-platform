import {
  Box,
  Checkbox,
  FormControlLabel,
  MenuItem,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { DangerButton } from 'tg.component/DangerZone/DangerButton';
import { DangerZone } from 'tg.component/DangerZone/DangerZone';
import { ProjectTransferModal } from './components/ProjectTransferModal';
import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useState } from 'react';
import { confirmation } from 'tg.hooks/confirmation';
import { ConfirmationDialogProps } from 'tg.component/common/ConfirmationDialog';
import { messageService } from 'tg.service/MessageService';
import { useHistory } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { Select } from 'tg.component/common/Select';
import { components } from 'tg.service/apiSchema.generated';

type EditProjectRequest = components['schemas']['EditProjectRequest'];
type SuggestionsMode =
  components['schemas']['EditProjectRequest']['suggestionsMode'];
type TranslationProtection =
  components['schemas']['EditProjectRequest']['translationProtection'];

export const ProjectSettingsAdvanced = () => {
  const project = useProject();
  const { t } = useTranslate();
  const history = useHistory();

  const deleteLoadable = useApiMutation({
    url: '/v2/projects/{projectId}',
    method: 'delete',
  });

  const updateLoadable = useApiMutation({
    url: '/v2/projects/{projectId}',
    method: 'put',
    invalidatePrefix: '/v2/projects',
  });

  const updateSettings = (values: Partial<EditProjectRequest>) => {
    updateLoadable.mutate({
      path: { projectId: project.id },
      content: {
        'application/json': {
          ...project,
          ...values,
        },
      },
    });
  };

  const [transferDialogOpen, setTransferDialogOpen] = useState(false);
  const confirm = (options: ConfirmationDialogProps) =>
    confirmation({
      title: <T keyName="delete_project_dialog_title" />,
      ...options,
    });

  const handleDelete = () => {
    confirm({
      message: (
        <T
          keyName="delete_project_confirmation_message"
          params={{ name: project.name }}
        />
      ),
      onConfirm: () =>
        deleteLoadable.mutate(
          { path: { projectId: project.id } },
          {
            onSuccess() {
              history.push(LINKS.AFTER_LOGIN.build());
              messageService.success(<T keyName="project_deleted_message" />);
            },
          }
        ),
      hardModeText: project.name.toUpperCase(),
    });
  };
  return (
    <>
      <Box mt={2} display="grid" justifyItems="start">
        <FormControlLabel
          control={
            <Checkbox
              disabled={updateLoadable.isLoading}
              checked={project.icuPlaceholders}
              onChange={(_, val) => updateSettings({ icuPlaceholders: val })}
            />
          }
          label={t('project_settings_use_tolgee_placeholders_label')}
          data-cy="project-settings-use-tolgee-placeholders-checkbox"
        />
        <Typography variant="caption">
          {t('project_settings_tolgee_placeholders_hint')}
        </Typography>
      </Box>

      <Box mt={2} display="grid" justifyItems="start">
        <Select
          label={t('project_settings_suggestions_mode_label')}
          value={project.suggestionsMode}
          data-cy="project-settings-suggestions-mode-select"
          minHeight={false}
          onChange={(e) =>
            updateSettings({
              suggestionsMode: e.target.value as SuggestionsMode,
            })
          }
          sx={{ paddingBottom: 1 }}
        >
          <MenuItem value="DISABLED">{t('suggestions_mode_disabled')}</MenuItem>
          <MenuItem value="ENABLED">{t('suggestions_mode_enabled')}</MenuItem>
        </Select>
        <Typography variant="caption">
          <T
            keyName="project_settings_suggestions_mode_hint"
            params={{ b: <b />, li: <li />, ul: <ul /> }}
          />
        </Typography>
      </Box>

      <Box mt={2} display="grid" justifyItems="start">
        <Select
          label={t('project_settings_translation_protection_label')}
          value={project.translationProtection}
          data-cy="project-settings-translation-protection-select"
          minHeight={false}
          onChange={(e) =>
            updateSettings({
              translationProtection: e.target.value as TranslationProtection,
            })
          }
          sx={{ paddingBottom: 1 }}
        >
          <MenuItem value="NONE">{t('translation_protection_none')}</MenuItem>
          <MenuItem value="PROTECT_REVIEWED">
            {t('translation_protection_protect_reviewed')}
          </MenuItem>
        </Select>
        <Typography variant="caption">
          <T
            keyName="project_settings_translation_protection_hint"
            params={{ b: <b />, li: <li />, ul: <ul /> }}
          />
        </Typography>
      </Box>

      <Box mt={4} mb={1}>
        <Typography variant={'h5'}>
          <T keyName="project_settings_danger_zone_title" />
        </Typography>
      </Box>
      <DangerZone
        actions={[
          {
            description: <T keyName="this_will_delete_project_forever" />,
            button: (
              <DangerButton
                onClick={handleDelete}
                data-cy="project-settings-delete-button"
              >
                <T keyName="delete_project_button" />
              </DangerButton>
            ),
          },
          {
            description: <T keyName="this_will_transfer_project" />,
            button: (
              <DangerButton
                onClick={() => setTransferDialogOpen(true)}
                data-cy="project-settings-transfer-button"
              >
                <T keyName="transfer_project_button" />
              </DangerButton>
            ),
          },
        ]}
      />

      <ProjectTransferModal
        open={transferDialogOpen}
        onClose={() => setTransferDialogOpen(false)}
      />
    </>
  );
};
