import { useCallback, useRef, useState } from 'react';
import { IconButton } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useHistory } from 'react-router-dom';

import { ArrowDropDown } from 'tg.component/CustomIcons';
import { ProjectSearchSelectItem } from 'tg.component/projectSearchSelect/ProjectSearchSelectItem';
import { SwitchPopover } from 'tg.component/SwitchPopover/SwitchPopover';
import { LINKS, PARAMS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';

type ProjectModel = components['schemas']['ProjectModel'];

type Props = {
  project: ProjectModel;
};

export const ProjectSwitch: React.FC<Props> = ({ project }) => {
  const anchorEl = useRef<HTMLButtonElement>(null);
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');
  const { t } = useTranslate();
  const history = useHistory();

  const slug = project.organizationOwner?.slug || '';

  const query = {
    search: search || undefined,
    size: 20,
    sort: ['name'],
  };

  const projectsLoadable = useApiInfiniteQuery({
    url: '/v2/organizations/{slug}/projects',
    method: 'get',
    path: { slug },
    query,
    options: {
      keepPreviousData: true,
      enabled: isOpen && Boolean(slug),
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: { slug },
            query: {
              ...query,
              page: lastPage.page!.number! + 1,
            },
          };
        } else {
          return null;
        }
      },
    },
  });

  const items = (projectsLoadable.data?.pages
    .flatMap((page) => page._embedded?.projects)
    .filter(Boolean) ?? []) as ProjectModel[];

  const totalElements =
    projectsLoadable.data?.pages[0]?.page?.totalElements ?? 0;

  const handleSearchChange = useCallback((value: string) => {
    setSearch(value);
  }, []);

  const handleSelect = (selected: ProjectModel) => {
    setIsOpen(false);
    history.push(
      LINKS.PROJECT_DASHBOARD.build({ [PARAMS.PROJECT_ID]: selected.id })
    );
  };

  return (
    <>
      <IconButton
        ref={anchorEl}
        sx={{ p: 0, color: 'inherit' }}
        onClick={() => setIsOpen(true)}
        data-cy="project-switch"
        aria-label={t('projects_switch_label', 'Switch project')}
        size="small"
      >
        <ArrowDropDown width={20} height={20} />
      </IconButton>

      <SwitchPopover
        open={isOpen}
        onClose={() => setIsOpen(false)}
        onSelect={handleSelect}
        anchorEl={anchorEl.current!}
        selectedId={project.id}
        items={items}
        isLoading={projectsLoadable.isFetching}
        hasNextPage={projectsLoadable.hasNextPage ?? false}
        fetchNextPage={() => projectsLoadable.fetchNextPage()}
        totalElements={totalElements}
        renderItem={(item) => <ProjectSearchSelectItem data={item} />}
        searchPlaceholder={t('projects_search_placeholder', 'Search projects')}
        headingText={t('projects_title', 'Projects')}
        onSearchChange={handleSearchChange}
      />
    </>
  );
};
