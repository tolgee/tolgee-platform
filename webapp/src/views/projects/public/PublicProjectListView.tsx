import { useEffect, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { styled } from '@mui/material';

import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import SearchField from 'tg.component/common/form/fields/SearchField';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useWindowTitle } from 'tg.hooks/useWindowTitle';
import { ProjectsList } from 'tg.views/projects/ProjectsList';
import { CommunityTranslationBanner } from './CommunityTranslationBanner';
import { PublicTopBar } from './PublicTopBar';
import { PUBLIC_CONTENT_MAX_WIDTH } from './publicProjectsLayout';

const StyledLayout = styled('div')`
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: ${({ theme }) => theme.palette.background.default};
`;

const StyledContent = styled('div')`
  container-type: inline-size;
  width: 100%;
  max-width: ${PUBLIC_CONTENT_MAX_WIDTH}px;
  margin: 0 auto;
  padding: ${({ theme }) => theme.spacing(4, 2)};
`;

const MAX_PROJECTS_WITHOUT_SEARCH = 5;

const StyledSearch = styled('div')`
  display: flex;
  padding: ${({ theme }) => theme.spacing(0, 0, 3)};
  & > * {
    width: 220px;
  }
`;

export const PublicProjectListView = () => {
  const { t } = useTranslate();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');

  useWindowTitle(t('public_projects_window_title', 'Community translations'));

  const projectsLoadable = useApiQuery({
    url: '/v2/public/projects/with-stats',
    method: 'get',
    query: {
      page,
      size: 20,
      search,
      sort: ['name,asc'],
    },
    options: {
      keepPreviousData: true,
    },
  });

  // Latch visibility once relevant: keepPreviousData holds the filtered (small) count while a cleared
  // search refetches, which would otherwise unmount the field mid-interaction and drop focus.
  const searchRelevant =
    Boolean(search) ||
    (projectsLoadable.data?.page?.totalElements ?? 0) > MAX_PROJECTS_WITHOUT_SEARCH;
  const [showSearch, setShowSearch] = useState(false);
  useEffect(() => {
    if (searchRelevant) {
      setShowSearch(true);
    }
  }, [searchRelevant]);

  return (
    <StyledLayout>
      <PublicTopBar />
      <CommunityTranslationBanner />
      <StyledContent>
        {showSearch && (
          <StyledSearch>
            <SearchField
              initial={search}
              onSearch={(value) => {
                setSearch(value);
                setPage(0);
              }}
              variant="outlined"
              size="small"
            />
          </StyledSearch>
        )}
        <ProjectsList
          variant="public"
          loadable={projectsLoadable}
          onPageChange={setPage}
          emptyPlaceholder={
            <EmptyListMessage loading={projectsLoadable.isFetching}>
              <T
                keyName="public_projects_empty"
                defaultValue="No public projects yet"
              />
            </EmptyListMessage>
          }
        />
      </StyledContent>
    </StyledLayout>
  );
};

export default PublicProjectListView;
