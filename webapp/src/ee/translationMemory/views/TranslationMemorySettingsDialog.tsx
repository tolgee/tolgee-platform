import React, { useMemo } from 'react';
import { Box, Dialog, DialogContent, DialogTitle } from '@mui/material';
import { T } from '@tolgee/react';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { messageService } from 'tg.service/MessageService';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import {
  CreateEditTranslationMemoryFormValues,
  TranslationMemoryCreateEditForm,
} from 'tg.ee.module/translationMemory/components/form/TranslationMemoryCreateEditForm';
import { PendingRemovalRow } from 'tg.ee.module/translationMemory/components/form/TmAssignedProjectsTable';

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
  translationMemoryId: number;
  projectsOnly?: boolean;
};

/**
 * Edit dialog for an existing TM. Wraps the same form used for creation
 * (TranslationMemoryCreateEditForm) — only the data plumbing differs:
 *   - initialValues come from the GET endpoint instead of empty defaults
 *   - submit calls PUT for the TM body, plus a per-project DELETE for any rows the user removed
 */
export const TranslationMemorySettingsDialog: React.VFC<Props> = ({
  open,
  onClose,
  onFinished,
  translationMemoryId,
  projectsOnly,
}) => {
  const { preferredOrganization } = usePreferredOrganization();
  const organizationId = preferredOrganization!.id;

  const tmQuery = useApiQuery({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}',
    method: 'get',
    path: { organizationId, translationMemoryId },
  });

  const assignedProjectsQuery = useApiQuery({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/assigned-projects',
    method: 'get',
    path: { organizationId, translationMemoryId },
  });

  const updateMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}',
    method: 'put',
    invalidatePrefix: '/v2/organizations/{organizationId}/translation-memories',
  });

  const unassignMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translation-memories/{translationMemoryId}',
    method: 'delete',
  });

  const initialValues = useMemo<
    CreateEditTranslationMemoryFormValues | undefined
  >(() => {
    if (!tmQuery.data || !assignedProjectsQuery.data) return undefined;
    const tag = tmQuery.data.sourceLanguageTag;
    // BaseLanguageSelect renders via name + flagEmoji; tag alone leaves the trigger empty.
    const info = languageInfo[tag];
    return {
      name: tmQuery.data.name,
      baseLanguage: {
        tag,
        name: info?.englishName || tag,
        flagEmoji: info?.flags?.[0],
      },
      defaultPenalty: tmQuery.data.defaultPenalty ?? 0,
      writeOnlyReviewed: tmQuery.data.writeOnlyReviewed ?? false,
      assignedProjects: (
        assignedProjectsQuery.data._embedded?.assignedProjects ?? []
      ).map((p) => ({
        projectId: p.projectId,
        projectName: p.projectName,
        readAccess: p.readAccess,
        writeAccess: p.writeAccess,
        penalty: p.penalty ?? null,
      })),
    };
  }, [tmQuery.data, assignedProjectsQuery.data]);

  const save = async (
    values: CreateEditTranslationMemoryFormValues,
    pendingRemovals: PendingRemovalRow[]
  ) => {
    try {
      // First fire per-project DELETEs. Doing this before the PUT avoids the case where the
      // PUT replaces assignments, dropping the row, and the subsequent DELETE then fails
      // because the assignment is gone.
      for (const removal of pendingRemovals) {
        await unassignMutation.mutateAsync({
          path: { projectId: removal.projectId, translationMemoryId },
        });
      }

      await updateMutation.mutateAsync({
        path: { organizationId, translationMemoryId },
        content: {
          'application/json': {
            name: values.name,
            sourceLanguageTag: values.baseLanguage?.tag ?? '',
            defaultPenalty: values.defaultPenalty,
            writeOnlyReviewed: values.writeOnlyReviewed,
            assignedProjects: values.assignedProjects.map((a) => ({
              projectId: a.projectId,
              readAccess: a.readAccess,
              writeAccess: a.writeAccess,
              penalty: a.penalty ?? undefined,
            })),
          },
        },
      });

      onFinished();
    } catch {
      messageService.error(
        <T
          keyName="translation_memory_save_settings_error"
          defaultValue="Failed to save settings"
        />
      );
    }
  };

  const isSaving = updateMutation.isLoading || unassignMutation.isLoading;
  const isLoading = tmQuery.isLoading || assignedProjectsQuery.isLoading;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      data-cy="tm-settings-dialog"
      onClick={(e) => e.stopPropagation()}
    >
      <DialogTitle>
        {projectsOnly ? (
          <T
            keyName="translation_memory_manage_projects_title"
            defaultValue="Manage projects"
          />
        ) : (
          <T
            keyName="translation_memory_settings_title"
            defaultValue="TM settings"
          />
        )}
      </DialogTitle>

      {isLoading || !initialValues ? (
        <DialogContent>
          <Box py={4} textAlign="center" color="text.secondary">
            <T keyName="global_loading" defaultValue="Loading..." />
          </Box>
        </DialogContent>
      ) : (
        <TranslationMemoryCreateEditForm
          mode="edit"
          initialValues={initialValues}
          onClose={onClose}
          onSave={save}
          isSaving={isSaving}
          projectsOnly={projectsOnly}
        />
      )}
    </Dialog>
  );
};
