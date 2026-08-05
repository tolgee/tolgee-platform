import { useState } from 'react';
import { styled, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useDebounce } from 'use-debounce';

import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { InfiniteMultiSearchSelect } from 'tg.component/searchSelect/InfiniteMultiSearchSelect';
import { MultiselectItem } from 'tg.component/searchSelect/MultiselectItem';
import { components } from 'tg.service/apiSchema.generated';
import { TranslatedError } from 'tg.translationTools/TranslatedError';

type ProjectModel = components['schemas']['ProjectModel'];

export type SelectableProject = {
  id: number;
  name: string;
};

const SEARCH_DEBOUNCE_MS = 500;
const PAGE_SIZE = 30;

const StyledEmpty = styled('div')`
  padding: ${({ theme }) => theme.spacing(2)};
  color: ${({ theme }) => theme.palette.text.secondary};
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  border: 1px solid ${({ theme }) => theme.palette.divider};
`;

type Props = {
  organizationId: number;
  selected: SelectableProject[];
  disabled?: boolean;
  onChange: (projects: SelectableProject[]) => void;
};

export const AppOrganizationProjectsSelect = ({
  organizationId,
  selected,
  disabled,
  onChange,
}: Props) => {
  const { t } = useTranslate();
  const [search, setSearch] = useState('');
  const [searchDebounced] = useDebounce(search, SEARCH_DEBOUNCE_MS);

  const query = {
    search: searchDebounced,
    size: PAGE_SIZE,
  };

  const projectsLoadable = useApiInfiniteQuery({
    url: '/v2/organizations/{id}/projects',
    method: 'get',
    path: { id: organizationId },
    query,
    options: {
      keepPreviousData: true,
      noGlobalLoading: true,
      // Reported inline on the select instead of as a global toast.
      onError: () => undefined,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: { id: organizationId },
            query: { ...query, page: lastPage.page!.number! + 1 },
          };
        }
        return null;
      },
    },
  });

  const items = projectsLoadable.data?.pages.flatMap(
    (page) => page._embedded?.projects ?? []
  );

  const totalElements = projectsLoadable.data?.pages[0]?.page?.totalElements;
  const organizationHasNoProjects = !searchDebounced && totalElements === 0;

  const handleFetchMore = () => {
    if (projectsLoadable.hasNextPage && !projectsLoadable.isFetching) {
      projectsLoadable.fetchNextPage();
    }
  };

  const toggleSelected = (project: ProjectModel) => {
    if (selected.some((item) => item.id === project.id)) {
      onChange(selected.filter((item) => item.id !== project.id));
      return;
    }
    onChange([...selected, { id: project.id, name: project.name }]);
  };

  const renderError = () => {
    if (!projectsLoadable.error) return undefined;
    if (typeof projectsLoadable.error.code === 'string') {
      return <TranslatedError code={projectsLoadable.error.code} />;
    }
    return <T keyName="simple_paginated_list_error_message" />;
  };

  if (organizationHasNoProjects) {
    return (
      <StyledEmpty data-cy="administration-apps-projects-empty">
        <Typography variant="body2">
          <T
            keyName="administration_apps_projects_empty"
            defaultValue="This organization has no projects yet. You can still grant it access — its projects can enable the app later."
          />
        </Typography>
      </StyledEmpty>
    );
  }

  return (
    <InfiniteMultiSearchSelect
      items={items}
      selected={selected}
      queryResult={projectsLoadable}
      itemKey={(project) => project.id}
      search={search}
      onSearchChange={setSearch}
      onFetchMore={handleFetchMore}
      onClearSelected={() => onChange([])}
      renderItem={(props, project) => (
        <MultiselectItem
          {...props}
          data-cy="administration-apps-projects-item"
          data-cy-project-id={project.id}
          selected={selected.some((item) => item.id === project.id)}
          label={project.name}
          onClick={() => toggleSelected(project)}
        />
      )}
      labelItem={(project) => project.name}
      label={t('administration_apps_projects_select_label', 'Projects')}
      searchPlaceholder={t(
        'administration_apps_projects_search_placeholder',
        'Search projects…'
      )}
      error={renderError()}
      disabled={disabled}
    />
  );
};
