import { useState } from 'react';
import { styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useHistory } from 'react-router-dom';

import { EmailNotVerifiedView } from 'tg.component/EmailNotVerifiedView';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { BaseView } from 'tg.component/layout/BaseView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { OrganizationSwitch } from 'tg.component/organizationSwitch/OrganizationSwitch';
import { LINKS } from 'tg.constants/links';
import { useIsEmailVerified } from 'tg.globalContext/helpers';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { ProjectsList } from 'tg.views/projects/ProjectsList';
import { CommunityTranslationBanner } from 'tg.views/projects/public/CommunityTranslationBanner';
import { useLatchedSearchVisibility } from 'tg.views/projects/useLatchedSearchVisibility';
import { CriticalUsageCircle } from 'tg.ee';

// Flex column so the full-width banner keeps its content height — the surrounding grid layouts
// (DashboardPage main + BaseView content) stretch a bare second grid row to fill the viewport.
const StyledContent = styled('div')`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(3)};
`;

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

  const showSearch = useLatchedSearchVisibility(
    loadable.data?.page?.totalElements,
    search
  );

  return (
    <DashboardPage>
      <BaseView
        data-cy="community-projects-view"
        windowTitle={t('community_projects_window_title')}
        title={t('projects_title')}
        standaloneTitle
        titleAdornment={
          <OrganizationSwitch
            plain
            selectedSurface="community"
            onSelect={() => history.push(LINKS.ROOT.build())}
          />
        }
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
        customButtons={[<CriticalUsageCircle key="usage" />]}
        loading={loadable.isFetching}
      >
        <StyledContent>
          <CommunityTranslationBanner />
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
        </StyledContent>
      </BaseView>
    </DashboardPage>
  );
};

export const CommunityProjectsView = () => {
  const isEmailVerified = useIsEmailVerified();
  return isEmailVerified ? <CommunityProjects /> : <EmailNotVerifiedView />;
};
