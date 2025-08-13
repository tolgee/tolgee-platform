import { Box, Typography } from '@mui/material';
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
import { components } from 'tg.service/apiSchema.generated';
import { SwitchWithDescription } from './components/SwitchWithDescription';
import { DOCS_ROOT } from 'tg.constants/docLinks';
import { useProjectNamespaces } from 'tg.hooks/useProjectNamespaces';
import { DefaultNamespaceSelect } from './components/DefaultNamespaceSelect';
import { LinkReadMore } from 'tg.component/LinkReadMore';
import { useReportEvent } from 'tg.hooks/useReportEvent';

type EditProjectRequest = components['schemas']['EditProjectRequest'];

export const ProjectSettingsAdvanced = () => {
  const project = useProject();
  const { t } = useTranslate();
  const history = useHistory();
  const reportEvent = useReportEvent();

  const { allNamespacesWithNone } = useProjectNamespaces();

  const deleteLoadable = useApiMutation({
    url: '/v2/projects/{projectId}',
    method: 'delete',
  });

  const updateLoadable = useApiMutation({
    url: '/v2/projects/{projectId}',
    method: 'put',
    invalidatePrefix: '/v2/projects',
  });

  const updateSettings = (
    values: Partial<EditProjectRequest>,
    onSuccess?: () => void
  ) => {
    updateLoadable.mutate(
      {
        path: { projectId: project.id },
        content: {
          'application/json': {
            ...project,
            ...values,
          },
        },
      },
      { onSuccess }
    );
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
    <Box display="grid" mb={8}>
      <Typography variant="h5" mt={4} mb="20px">
        {t('project_settings_advanced_translations')}
      </Typography>

      <SwitchWithDescription
        data-cy="project-settings-suggestions-mode-switch"
        title={t('project_settings_suggestions_mode_label')}
        description={
          <T
            keyName="project_settings_suggestions_mode_hint"
            params={{ b: <b />, li: <li />, ul: <ul /> }}
          />
        }
        checked={project.suggestionsMode === 'ENABLED'}
        onSwitch={() => {
          const value =
            project.suggestionsMode === 'ENABLED' ? 'DISABLED' : 'ENABLED';
          updateSettings(
            {
              suggestionsMode: value,
            },
            () => reportEvent('PROJECT_SUGGESTIONS_SETTINGS_CHANGE', { value })
          );
        }}
        disabled={updateLoadable.isLoading}
      />

      <Box mt={2} />

      <SwitchWithDescription
        data-cy="project-settings-translation-protection-switch"
        title={t('project_settings_translation_protection_label')}
        description={
          <T
            keyName="project_settings_translation_protection_hint"
            params={{ b: <b />, li: <li />, ul: <ul /> }}
          />
        }
        checked={project.translationProtection === 'PROTECT_REVIEWED'}
        onSwitch={() => {
          const value =
            project.translationProtection === 'PROTECT_REVIEWED'
              ? 'NONE'
              : 'PROTECT_REVIEWED';
          updateSettings(
            {
              translationProtection: value,
            },
            () =>
              reportEvent('PROJECT_TRANSLATION_PROTECTION_SETTINGS_CHANGE', {
                value,
              })
          );
        }}
        disabled={updateLoadable.isLoading}
      />

      <Typography variant="h5" mt={5} mb="20px">
        {t('project_settings_advanced_export_and_file_formats')}
      </Typography>

      <SwitchWithDescription
        data-cy="project-settings-use-tolgee-placeholders-switch"
        title={t('project_settings_use_tolgee_placeholders_label')}
        description={
          <T
            keyName="project_settings_tolgee_placeholders_hint"
            params={{
              LearnMore: (
                <LinkReadMore
                  url={`${DOCS_ROOT}/platform/translation_process/tolgee_universal_icu_placeholders`}
                />
              ),
            }}
          />
        }
        checked={project.icuPlaceholders}
        onSwitch={() =>
          updateSettings({ icuPlaceholders: !project.icuPlaceholders })
        }
        disabled={updateLoadable.isLoading}
      />

      <Box pt={2} />

      <SwitchWithDescription
        data-cy="project-settings-use-namespaces-switch"
        title={t('project_settings_use_namespaces')}
        description={
          <T
            keyName="project_settings_use_namespaces_hint"
            params={{
              LearnMore: (
                <LinkReadMore url={`${DOCS_ROOT}/js-sdk/namespaces`} />
              ),
            }}
          />
        }
        checked={project.useNamespaces}
        onSwitch={() =>
          updateSettings({
            useNamespaces: !project.useNamespaces,
          })
        }
        disabled={updateLoadable.isLoading}
      />

      {project.useNamespaces && (
        <Box display="grid" pt={2} maxWidth="300px">
          <DefaultNamespaceSelect
            data-cy="project-settings-use-namespaces-default-namespace-select"
            label={<T keyName="project_settings_base_namespace" />}
            name="defaultNamespaceId"
            namespaces={allNamespacesWithNone}
            hidden={!project.useNamespaces}
            value={project.defaultNamespace?.id}
            onChange={(nsId) => updateSettings({ defaultNamespaceId: nsId })}
          />
        </Box>
      )}

      <Typography variant="h5" mt={5} mb={2}>
        <T keyName="project_settings_danger_zone_title" />
      </Typography>

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
    </Box>
  );
};
