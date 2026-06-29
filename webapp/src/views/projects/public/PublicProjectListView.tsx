import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { styled } from '@mui/material';

import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useWindowTitle } from 'tg.hooks/useWindowTitle';
import { ProjectsList } from 'tg.views/projects/ProjectsList';
import { CommunityTranslationBanner } from './CommunityTranslationBanner';
import { PublicTopBar } from './PublicTopBar';

const StyledLayout = styled('div')`
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: ${({ theme }) => theme.palette.background.default};
`;

const StyledContent = styled('div')`
  container-type: inline-size;
  width: 100%;
  max-width: 1000px;
  margin: 0 auto;
  padding: ${({ theme }) => theme.spacing(0, 2, 4)};
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

  return (
    <StyledLayout>
      <PublicTopBar />
      <StyledContent>
        <CommunityTranslationBanner />
        <ProjectsList
          variant="public"
          loadable={projectsLoadable}
          onPageChange={setPage}
          onSearchChange={(value) => {
            setSearch(value);
            setPage(0);
          }}
          searchText={search}
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
