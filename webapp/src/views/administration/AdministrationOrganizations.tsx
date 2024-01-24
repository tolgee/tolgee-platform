import { useState } from 'react';
import { Link, useHistory } from 'react-router-dom';
import { T, useTranslate } from '@tolgee/react';
import {
  Box,
  Button,
  Chip,
  ListItem,
  ListItemSecondaryAction,
  ListItemText,
  styled,
} from '@mui/material';

import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { LINKS, PARAMS } from 'tg.constants/links';
import { BaseAdministrationView } from './components/BaseAdministrationView';

const StyledWrapper = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;

  & .listWrapper > * > * + * {
    border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  }
`;

export const AdministrationOrganizations = ({
  search,
  setSearch,
}: {
  search: string;
  setSearch: (str: string) => void;
}) => {
  const [page, setPage] = useState(0);
  const { preferredOrganization, updatePreferredOrganization } =
    usePreferredOrganization();

  const listPermitted = useApiQuery({
    url: '/v2/administration/organizations',
    method: 'get',
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

  const history = useHistory();
  const { t } = useTranslate();

  return (
    <StyledWrapper>
      <DashboardPage>
        <BaseAdministrationView
          windowTitle={t('administration_organizations')}
          navigation={[
            [
              t('administration_organizations'),
              LINKS.ADMINISTRATION_ORGANIZATIONS.build(),
            ],
          ]}
          initialSearch={search}
          allCentered
          hideChildrenOnLoading={false}
          loading={listPermitted.isFetching}
        >
          <PaginatedHateoasList
            wrapperComponentProps={{ className: 'listWrapper' }}
            onPageChange={setPage}
            onSearchChange={setSearch}
            searchText={search}
            loadable={listPermitted}
            renderItem={(o) => (
              <ListItem data-cy="administration-organizations-list-item">
                <ListItemText>
                  {o.name} <Chip size="small" label={o.id} />
                </ListItemText>
                <ListItemSecondaryAction>
                  <Box display="flex">
                    <Button
                      data-cy="administration-organizations-projects-button"
                      variant="contained"
                      onClick={() => {
                        updatePreferredOrganization(o.id);
                        history.push(LINKS.PROJECTS.build());
                      }}
                    >
                      <T keyName="administration_organization_projects" />
                    </Button>
                    <Box ml={1}>
                      <Button
                        data-cy="administration-organizations-settings-button"
                        variant="contained"
                        component={Link}
                        to={LINKS.ORGANIZATION_PROFILE.build({
                          [PARAMS.ORGANIZATION_SLUG]: o.slug,
                        })}
                      >
                        <T keyName="administration_organizations_settings" />
                      </Button>
                    </Box>
                  </Box>
                </ListItemSecondaryAction>
              </ListItem>
            )}
          />
        </BaseAdministrationView>
      </DashboardPage>
    </StyledWrapper>
  );
};
