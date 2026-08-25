import { useCallback, useRef, useState } from 'react';
import { Box, IconButton, Link, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Link as RouterLink, useHistory } from 'react-router-dom';

import { useBranchLinks } from 'tg.component/branching/useBranchLinks';
import { ArrowDropDown } from 'tg.component/CustomIcons';
import { SmallProjectAvatar } from 'tg.component/navigation/SmallProjectAvatar';
import { ProjectSearchSelectItem } from 'tg.component/projectSearchSelect/ProjectSearchSelectItem';
import { SwitchPopover } from 'tg.component/SwitchPopover/SwitchPopover';
import { LINKS, PARAMS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';

type ProjectModel = components['schemas']['ProjectModel'];

const StyledLink = styled(Link)`
  display: grid;
  grid-auto-flow: column;
  align-items: center;
  gap: 8px;
`;

type Props = {
  project: ProjectModel;
};

export const ProjectSwitch: React.FC<Props> = ({ project }) => {
  const anchorEl = useRef<HTMLDivElement>(null);
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');
  const { t } = useTranslate();
  const history = useHistory();
  const { withBranchLink } = useBranchLinks();

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
    if (selected.id === project.id) {
      return;
    }
    history.push(
      LINKS.PROJECT_DASHBOARD.build({ [PARAMS.PROJECT_ID]: selected.id })
    );
  };

  return (
    <>
      <Box ref={anchorEl} display="flex" alignItems="center" gap={1}>
        <StyledLink
          // @ts-ignore
          to={withBranchLink(LINKS.PROJECT_DASHBOARD, {
            [PARAMS.PROJECT_ID]: project.id,
          })}
          component={RouterLink}
        >
          <SmallProjectAvatar project={project} />
          {project.name}
        </StyledLink>
        <IconButton
          sx={{
            p: 0,
            color: 'primary.main',
            '&:hover': { bgcolor: 'transparent' },
          }}
          onClick={() => setIsOpen(true)}
          data-cy="project-switch"
          aria-label={t('projects_switch_label', 'Switch project')}
          aria-haspopup="listbox"
          aria-expanded={isOpen}
          size="small"
        >
          <ArrowDropDown width={20} height={20} />
        </IconButton>
      </Box>

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
