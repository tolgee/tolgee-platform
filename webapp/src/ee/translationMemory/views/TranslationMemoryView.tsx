import { LINKS, PARAMS } from 'tg.constants/links';
import { BaseOrganizationSettingsView } from 'tg.views/organizations/components/BaseOrganizationSettingsView';
import { T, useTranslate } from '@tolgee/react';
import React, { useMemo, useState } from 'react';
import { Box, IconButton, styled, Tooltip, Typography } from '@mui/material';
import { Settings01 } from '@untitled-ui/icons-react';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import {
  useEnabledFeatures,
  useIsOrganizationOwnerOrMaintainer,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useRouteMatch, Redirect } from 'react-router-dom';
import { TranslationMemoryEntriesList } from 'tg.ee.module/translationMemory/components/content/TranslationMemoryEntriesList';
import { TranslationMemorySettingsDialog } from 'tg.ee.module/translationMemory/views/TranslationMemorySettingsDialog';
import { TmWriteOnlyReviewedDialog } from 'tg.ee.module/translationMemory/views/TmWriteOnlyReviewedDialog';
import { messageService } from 'tg.service/MessageService';
import { ProjectLink } from 'tg.component/ProjectLink';

const StyledSharedWith = styled(Box)`
  display: flex;
  align-items: center;
  gap: 4px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

export const TranslationMemoryView = () => {
  const [search, setSearch] = useUrlSearchState('search', {
    defaultVal: '',
  });
  const [settingsOpen, setSettingsOpen] = useState(false);

  const canManage = useIsOrganizationOwnerOrMaintainer();
  const { isEnabled } = useEnabledFeatures();
  const { preferredOrganization } = usePreferredOrganization();
  const organization = useOrganization();
  const match = useRouteMatch();
  const translationMemoryId = Number(
    match.params[PARAMS.TRANSLATION_MEMORY_ID]
  );

  const { t } = useTranslate();

  const tm = useApiQuery({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}',
    method: 'get',
    path: {
      organizationId: organization!.id,
      translationMemoryId,
    },
    options: {
      enabled: !isNaN(translationMemoryId),
    },
  });

  const writeOnlyReviewedMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/write-only-reviewed',
    method: 'put',
    invalidatePrefix: '/v2/organizations/{organizationId}/translation-memories',
  });

  const assignedProjects = useApiQuery({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/assigned-projects',
    method: 'get',
    path: {
      organizationId: organization!.id,
      translationMemoryId,
    },
    options: {
      enabled: !isNaN(translationMemoryId),
    },
  });

  const tmData = tm.data;
  const assignedProjectsList =
    assignedProjects.data?._embedded?.assignedProjects ?? [];
  const assignedProjectsCount = assignedProjectsList.length;

  const projectsForInfo = useMemo(
    () =>
      assignedProjectsList.map((project) => ({
        id: project.projectId,
        name: project.projectName,
      })),
    [assignedProjectsList]
  );

  if (isNaN(translationMemoryId) || !isEnabled('TRANSLATION_MEMORY')) {
    // Direct-URL access to the org TM view falls through when the feature is disabled.
    // The list view renders the upgrade banner — bounce there instead of showing a broken
    // detail page that depends on TM data the API won't return.
    return (
      <Redirect
        to={LINKS.ORGANIZATION_TRANSLATION_MEMORIES.build({
          [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
        })}
      />
    );
  }

  return (
    <BaseOrganizationSettingsView
      windowTitle={
        tmData?.name ||
        t('organization_translation_memory_title', 'Translation memory')
      }
      title={
        tmData?.name ||
        t('organization_translation_memory_title', 'Translation memory')
      }
      titleAdornment={
        <StyledSharedWith data-cy="translation-memory-shared-with">
          <Typography variant="body2" component="span">
            {tmData?.type === 'PROJECT' ? (
              <T
                keyName="translation_memory_project_tm_label"
                defaultValue="(Project translation memory)"
              />
            ) : projectsForInfo.length === 0 ? (
              <T
                keyName="translation_memory_not_shared_label"
                defaultValue="(Not shared with any project)"
              />
            ) : (
              <>
                (
                <T
                  keyName="translation_memory_shared_with_label"
                  defaultValue="Shared with:"
                />{' '}
                {projectsForInfo.slice(0, 2).map((project, index) => (
                  <span key={project.id}>
                    {index > 0 && ', '}
                    <ProjectLink project={project} />
                  </span>
                ))}
                {projectsForInfo.length > 2 && (
                  <Tooltip
                    placement="bottom-end"
                    title={
                      <Typography variant="body2" component="span">
                        {projectsForInfo.slice(2).map((project) => (
                          <div key={project.id}>
                            <ProjectLink project={project} />
                          </div>
                        ))}
                      </Typography>
                    }
                  >
                    <span>, ...</span>
                  </Tooltip>
                )}
                )
              </>
            )}
          </Typography>
          {canManage && (
            <Tooltip
              title={t('translation_memory_settings_button', 'TM settings')}
            >
              <IconButton
                size="small"
                onClick={() => setSettingsOpen(true)}
                data-cy="translation-memory-settings-button"
              >
                <Settings01 width={18} height={18} />
              </IconButton>
            </Tooltip>
          )}
        </StyledSharedWith>
      }
      link={LINKS.ORGANIZATION_TRANSLATION_MEMORY}
      navigation={[
        [
          t('organization_translation_memories_title', 'Translation memories'),
          LINKS.ORGANIZATION_TRANSLATION_MEMORIES.build({
            [PARAMS.ORGANIZATION_SLUG]: preferredOrganization?.slug || '',
          }),
        ],
        [tmData?.name || '...'],
      ]}
      maxWidth="max"
      allCentered={false}
      loading={tm.isLoading}
      hideChildrenOnLoading={false}
    >
      {tmData && (
        <TranslationMemoryEntriesList
          organizationId={organization!.id}
          translationMemoryId={translationMemoryId}
          sourceLanguageTag={tmData.sourceLanguageTag}
          tmName={tmData.name}
          defaultPenalty={
            (tmData as { defaultPenalty?: number }).defaultPenalty ?? 0
          }
          assignedProjectsCount={assignedProjectsCount}
          isProjectTm={tmData.type === 'PROJECT'}
          search={search}
          onSearch={setSearch}
        />
      )}
      {settingsOpen && tmData?.type === 'PROJECT' && (
        <TmWriteOnlyReviewedDialog
          open={settingsOpen}
          initialWriteOnlyReviewed={tmData.writeOnlyReviewed ?? false}
          saving={writeOnlyReviewedMutation.isLoading}
          onClose={() => setSettingsOpen(false)}
          onSave={(writeOnlyReviewed) =>
            writeOnlyReviewedMutation.mutate(
              {
                path: {
                  organizationId: organization!.id,
                  translationMemoryId,
                },
                content: { 'application/json': { writeOnlyReviewed } },
              },
              {
                onSuccess: () => {
                  setSettingsOpen(false);
                  tm.refetch();
                },
                onError: () =>
                  messageService.error(
                    <T
                      keyName="translation_memory_save_settings_error"
                      defaultValue="Failed to save settings"
                    />
                  ),
              }
            )
          }
        />
      )}
      {settingsOpen && tmData?.type !== 'PROJECT' && (
        <TranslationMemorySettingsDialog
          open={settingsOpen}
          onClose={() => setSettingsOpen(false)}
          onFinished={() => {
            setSettingsOpen(false);
            assignedProjects.refetch();
            tm.refetch();
          }}
          translationMemoryId={translationMemoryId}
        />
      )}
    </BaseOrganizationSettingsView>
  );
};
