import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';

import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { BaseView } from 'tg.component/layout/BaseView';
import { BaseViewAddButton } from 'tg.component/layout/BaseViewAddButton';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { ProjectsList } from 'tg.views/projects/ProjectsList';
import { Button } from '@mui/material';
import { Link } from 'react-router-dom';
import {
  useIsAdminOrSupporter,
  useIsOrganizationOwnerOrMaintainer,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { OrganizationSwitch } from 'tg.component/organizationSwitch/OrganizationSwitch';
import { useLatchedSearchVisibility } from 'tg.views/projects/useLatchedSearchVisibility';
import { QuickStartHighlight } from 'tg.component/layout/QuickStartGuide/QuickStartHighlight';
import { CriticalUsageCircle } from 'tg.ee';

export const ProjectListView = () => {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const { preferredOrganization } = usePreferredOrganization();

  const listPermitted = useApiQuery({
    url: '/v2/organizations/{slug}/projects-with-stats',
    method: 'get',
    path: { slug: preferredOrganization?.slug || '' },
    query: {
      page,
      size: 20,
      search,
      sort: ['id,desc'],
    },
    options: {
      keepPreviousData: true,
      enabled: Boolean(preferredOrganization?.slug),
    },
  });

  const { t } = useTranslate();

  const isOrganizationOwnerOrMaintainer = useIsOrganizationOwnerOrMaintainer();

  const isAdminOrSupporter = useIsAdminOrSupporter();

  const isAdminAccess =
    !preferredOrganization?.currentUserRole && isAdminOrSupporter;

  const addAllowed = isOrganizationOwnerOrMaintainer || isAdminAccess;

  const showSearch = useLatchedSearchVisibility(
    listPermitted.data?.page?.totalElements,
    search
  );

  return (
    <DashboardPage isAdminAccess={isAdminAccess}>
      <BaseView
        windowTitle={t('projects_title')}
        title={t('projects_title')}
        titleAdornment={<OrganizationSwitch plain />}
        standaloneTitle
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
        addComponent={
          addAllowed && (
            <QuickStartHighlight itemKey="add_project">
              <BaseViewAddButton
                addLinkTo={LINKS.PROJECT_ADD.build()}
                label={t('projects_add_button')}
              />
            </QuickStartHighlight>
          )
        }
        addLabel={t('projects_add_button')}
        hideChildrenOnLoading={false}
        customButtons={[<CriticalUsageCircle key="usage" />]}
        loading={listPermitted.isFetching}
      >
        <ProjectsList
          loadable={listPermitted}
          onPageChange={setPage}
          emptyPlaceholder={
            <EmptyListMessage
              loading={listPermitted.isFetching}
              hint={
                isOrganizationOwnerOrMaintainer ? (
                  <Button
                    component={Link}
                    to={LINKS.PROJECT_ADD.build()}
                    color="primary"
                  >
                    <T keyName="projects_empty_action" />
                  </Button>
                ) : undefined
              }
            >
              <T keyName="projects_empty" />
            </EmptyListMessage>
          }
        />
      </BaseView>
    </DashboardPage>
  );
};
