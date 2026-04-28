import { Box, Button, styled, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import {
  useEnabledFeatures,
  useIsOrganizationOwnerOrMaintainer,
} from 'tg.globalContext/helpers';
import { DisabledFeatureBanner } from 'tg.component/common/DisabledFeatureBanner';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useHistory } from 'react-router-dom';
import { useQueryClient } from 'react-query';
import { useEffect, useMemo, useState } from 'react';
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
} from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { restrictToVerticalAxis } from '@dnd-kit/modifiers';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationMemorySettingsDialog } from 'tg.ee.module/translationMemory/views/TranslationMemorySettingsDialog';
import { ProjectTmAssignmentDialog } from 'tg.ee.module/translationMemory/views/ProjectTmAssignmentDialog';
import { TmWriteOnlyReviewedDialog } from 'tg.ee.module/translationMemory/views/TmWriteOnlyReviewedDialog';
import { messageService } from 'tg.service/MessageService';
import { SortableTmRow } from './SortableTmRow';

type AssignmentModel =
  components['schemas']['ProjectTranslationMemoryAssignmentModel'];

const StyledTable = styled('div')`
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-radius: 8px;
  overflow: hidden;
  margin-top: 8px;
`;

const StyledHeader = styled('div')`
  display: grid;
  grid-template-columns: 28px 40px 1fr auto 36px;
  align-items: center;
  gap: 8px;
  padding: ${({ theme }) => theme.spacing(0.75, 1.5)};
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: ${({ theme }) => theme.palette.text.secondary};
  font-weight: 500;
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
`;

export const ProjectSettingsTranslationMemory = () => {
  const project = useProject();
  const { t } = useTranslate();
  const history = useHistory();
  const queryClient = useQueryClient();
  const { isEnabled } = useEnabledFeatures();
  const featureEnabled = isEnabled('TRANSLATION_MEMORY');
  const { satisfiesPermission } = useProjectPermissions();
  const canEditProject = satisfiesPermission('project.edit');
  const isOrgMaintainer = useIsOrganizationOwnerOrMaintainer();
  const [settingsTmId, setSettingsTmId] = useState<number | null>(null);

  const assignments = useApiQuery({
    url: '/v2/projects/{projectId}/translation-memories',
    method: 'get',
    path: { projectId: project.id },
    options: {
      enabled: featureEnabled,
    },
  });

  const serverSorted = useMemo(
    () =>
      [
        ...(assignments.data?._embedded?.translationMemoryAssignments ?? []),
      ].sort((a, b) => a.priority - b.priority),
    [assignments.data]
  );

  // Optimistic order used while the reorder PUTs are in flight. Cleared when
  // fresh server data arrives (react-query's `data` identity changes).
  const [optimisticOrder, setOptimisticOrder] = useState<
    AssignmentModel[] | null
  >(null);
  useEffect(() => {
    setOptimisticOrder(null);
  }, [assignments.data]);

  const sorted = optimisticOrder ?? serverSorted;

  const orgSlug = project.organizationOwner?.slug;

  const handleManage = () => {
    if (orgSlug) {
      history.push(
        LINKS.ORGANIZATION_TRANSLATION_MEMORIES.build({
          [PARAMS.ORGANIZATION_SLUG]: orgSlug,
        })
      );
    }
  };

  const invalidate = () =>
    queryClient.invalidateQueries(
      '/v2/projects/{projectId}/translation-memories'
    );

  const updateMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translation-memories/{translationMemoryId}',
    method: 'put',
  });

  const projectTmSettingsMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translation-memories/project-tm-settings',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/translation-memories',
  });

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor)
  );

  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over || active.id === over.id) return;

    const oldIndex = sorted.findIndex(
      (tm) => tm.translationMemoryId === active.id
    );
    const newIndex = sorted.findIndex(
      (tm) => tm.translationMemoryId === over.id
    );
    if (oldIndex === -1 || newIndex === -1) return;

    const a = sorted[oldIndex];
    const b = sorted[newIndex];

    // Show the new order immediately — without this the @dnd-kit drop animation
    // snaps the row back to its old position until the async PUTs resolve + the
    // query refetches.
    setOptimisticOrder(arrayMove(sorted, oldIndex, newIndex));

    try {
      await updateMutation.mutateAsync({
        path: {
          projectId: project.id,
          translationMemoryId: a.translationMemoryId,
        },
        content: {
          'application/json': {
            readAccess: a.readAccess,
            writeAccess: a.writeAccess,
            priority: b.priority,
            penalty: a.penalty ?? undefined,
          },
        },
      });
      await updateMutation.mutateAsync({
        path: {
          projectId: project.id,
          translationMemoryId: b.translationMemoryId,
        },
        content: {
          'application/json': {
            readAccess: b.readAccess,
            writeAccess: b.writeAccess,
            priority: a.priority,
            penalty: b.penalty ?? undefined,
          },
        },
      });
    } catch {
      // On failure roll back to the server order.
      setOptimisticOrder(null);
    }
    invalidate();
  };

  const settingsAssignment =
    settingsTmId !== null
      ? sorted.find((tm) => tm.translationMemoryId === settingsTmId)
      : undefined;

  const closeSettings = () => setSettingsTmId(null);
  const finishSettings = () => {
    closeSettings();
    invalidate();
  };

  return (
    <>
      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        mt={5}
        mb={1}
      >
        <Typography variant="h5">
          {t('project_settings_translation_memory_title', 'Translation memory')}
        </Typography>
        {featureEnabled && orgSlug && canEditProject && (
          <Button
            variant="outlined"
            color="primary"
            size="small"
            onClick={handleManage}
            data-cy="project-settings-tm-configure"
          >
            {t('project_settings_tm_manage_all', 'Manage all TMs')}
          </Button>
        )}
      </Box>

      {!featureEnabled ? (
        <DisabledFeatureBanner
          customMessage={t(
            'translation_memories_feature_description',
            'Translation memory management is available on the Business plan.'
          )}
        />
      ) : (
        <Box data-cy="project-settings-tm-shared">
          <Typography variant="body2" color="text.secondary" mb={1}>
            <T
              keyName="project_settings_tm_description"
              defaultValue="Control which translation memories are active in this project and set their priority. Memories higher in the list are preferred when suggesting translations in the editor."
            />
          </Typography>

          {sorted.length > 0 && (
            <>
              {canEditProject && (
                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{ display: 'block', mb: 0.5 }}
                >
                  {t(
                    'project_settings_tm_priority_hint',
                    'Priority order — drag to reorder'
                  )}
                </Typography>
              )}
              <StyledTable data-cy="project-settings-tm-table">
                <StyledHeader>
                  <span />
                  <span>{t('project_settings_tm_col_priority', '#')}</span>
                  <span>
                    {t('project_settings_tm_col_name', 'Translation memory')}
                  </span>
                  <span>{t('project_settings_tm_col_access', 'Access')}</span>
                  <span />
                </StyledHeader>
                <DndContext
                  sensors={sensors}
                  collisionDetection={closestCenter}
                  modifiers={[restrictToVerticalAxis]}
                  onDragEnd={handleDragEnd}
                >
                  <SortableContext
                    items={sorted.map((tm) => tm.translationMemoryId)}
                    strategy={verticalListSortingStrategy}
                  >
                    {sorted.map((tm, index) => (
                      <SortableTmRow
                        key={tm.translationMemoryId}
                        tm={tm}
                        index={index}
                        onSettings={() =>
                          setSettingsTmId(tm.translationMemoryId)
                        }
                        canEdit={canEditProject}
                      />
                    ))}
                  </SortableContext>
                </DndContext>
              </StyledTable>
            </>
          )}
        </Box>
      )}

      {settingsTmId !== null && settingsAssignment?.type === 'PROJECT' && (
        <TmWriteOnlyReviewedDialog
          open
          initialWriteOnlyReviewed={settingsAssignment.writeOnlyReviewed}
          saving={projectTmSettingsMutation.isLoading}
          onSave={(writeOnlyReviewed) =>
            projectTmSettingsMutation.mutate(
              {
                path: { projectId: project.id },
                content: { 'application/json': { writeOnlyReviewed } },
              },
              {
                onSuccess: finishSettings,
                onError: () => messageService.error('Failed to save settings'),
              }
            )
          }
          onClose={closeSettings}
        />
      )}

      {settingsTmId !== null &&
        settingsAssignment?.type !== 'PROJECT' &&
        isOrgMaintainer && (
          <TranslationMemorySettingsDialog
            open
            translationMemoryId={settingsTmId}
            onClose={closeSettings}
            onFinished={finishSettings}
          />
        )}

      {settingsTmId !== null &&
        settingsAssignment?.type !== 'PROJECT' &&
        !isOrgMaintainer &&
        settingsAssignment && (
          <ProjectTmAssignmentDialog
            open
            projectId={project.id}
            assignment={settingsAssignment}
            onClose={closeSettings}
            onFinished={finishSettings}
          />
        )}
    </>
  );
};
