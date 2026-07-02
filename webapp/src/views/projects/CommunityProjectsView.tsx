import { useEffect, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { useHistory } from 'react-router-dom';

import { EmailNotVerifiedView } from 'tg.component/EmailNotVerifiedView';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { BaseView } from 'tg.component/layout/BaseView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { ProjectsListTitle } from 'tg.views/projects/ProjectsListTitle';
import { LINKS } from 'tg.constants/links';
import { useIsEmailVerified } from 'tg.globalContext/helpers';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { ProjectsList } from 'tg.views/projects/ProjectsList';
import { CriticalUsageCircle } from 'tg.ee';

const MAX_PROJECTS_WITHOUT_SEARCH = 5;

const CommunityProjects = () => {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const { t } = useTranslate();
  const history = useHistory();

  const loadable = useApiQuery({
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

  // Latch visibility once relevant: keepPreviousData holds the filtered (small) count while a
  // cleared search refetches, which would otherwise unmount the field mid-interaction and drop focus.
  const searchRelevant =
    Boolean(search) ||
    (loadable.data?.page?.totalElements ?? 0) > MAX_PROJECTS_WITHOUT_SEARCH;
  const [showSearch, setShowSearch] = useState(false);
  useEffect(() => {
    if (searchRelevant) {
      setShowSearch(true);
    }
  }, [searchRelevant]);

  return (
    <DashboardPage>
      <BaseView
        data-cy="community-projects-view"
        windowTitle={t('community_projects_window_title')}
        onSearch={
          showSearch
            ? (value) => {
                setSearch(value);
                setPage(0);
              }
            : undefined
        }
        searchPlaceholder={t('projects_search_placeholder')}
        maxWidth={1000}
        allCentered
        hideChildrenOnLoading={false}
        navigation={[
          [
            <ProjectsListTitle
              key={0}
              communityNavigation
              selectedSurface="community"
              onSelect={() => history.push(LINKS.ROOT.build())}
            />,
          ],
        ]}
        navigationRight={<CriticalUsageCircle />}
        loading={loadable.isFetching}
      >
        <ProjectsList
          variant="public"
          loadable={loadable}
          onPageChange={setPage}
          emptyPlaceholder={
            <EmptyListMessage loading={loadable.isFetching}>
              <T keyName="public_projects_empty" />
            </EmptyListMessage>
          }
        />
      </BaseView>
    </DashboardPage>
  );
};

export const CommunityProjectsView = () => {
  const isEmailVerified = useIsEmailVerified();
  return isEmailVerified ? <CommunityProjects /> : <EmailNotVerifiedView />;
};
