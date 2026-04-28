import { LINKS, PARAMS } from 'tg.constants/links';
import { BaseOrganizationSettingsView } from 'tg.views/organizations/components/BaseOrganizationSettingsView';
import { useTranslate } from '@tolgee/react';
import React, { useMemo } from 'react';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useRouteMatch, Redirect } from 'react-router-dom';
import { TranslationMemoryEntriesList } from 'tg.ee.module/translationMemory/components/TranslationMemoryEntriesList';
import { ProjectsUsedInfo } from 'tg.component/ProjectsUsedInfo';

export const TranslationMemoryView = () => {
  const [search, setSearch] = useUrlSearchState('search', {
    defaultVal: '',
  });

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

  if (isNaN(translationMemoryId)) {
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
        <ProjectsUsedInfo
          projects={projectsForInfo}
          label={t(
            'translation_memory_projects_used_in_label',
            'Used in projects:'
          )}
          data-cy="translation-memory-projects-info"
        />
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
          search={search}
          onSearch={setSearch}
        />
      )}
    </BaseOrganizationSettingsView>
  );
};
