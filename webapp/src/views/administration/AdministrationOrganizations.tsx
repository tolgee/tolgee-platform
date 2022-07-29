import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { BaseView } from 'tg.component/layout/BaseView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import {
  Box,
  Button,
  ListItem,
  ListItemSecondaryAction,
  styled,
} from '@mui/material';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import ListItemText from '@mui/material/ListItemText';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { AdministrationNav } from './AdministrationNav';

const StyledWrapper = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;

  & .listWrapper > * > * + * {
    border-top: 1px solid ${({ theme }) => theme.palette.divider1.main};
  }
`;

export const AdministrationOrganizations = () => {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
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

  const t = useTranslate();

  return (
    <StyledWrapper>
      <DashboardPage>
        <BaseView
          windowTitle={t('administration_organizations')}
          onSearch={setSearch}
          containerMaxWidth="lg"
          allCentered
          hideChildrenOnLoading={false}
          loading={listPermitted.isFetching}
        >
          <AdministrationNav />

          <PaginatedHateoasList
            wrapperComponentProps={{ className: 'listWrapper' }}
            onPageChange={setPage}
            loadable={listPermitted}
            renderItem={(o) => (
              <ListItem>
                <ListItemText>{o.name}</ListItemText>
                <ListItemSecondaryAction>
                  <Box display="flex">
                    <Button
                      variant="contained"
                      onClick={() => {
                        updatePreferredOrganization(o);
                        location.replace(LINKS.PROJECTS.build());
                      }}
                    >
                      <T>administration_organization_projects</T>
                    </Button>
                    <Box ml={1}>
                      <Button
                        variant="contained"
                        component={Link}
                        to={LINKS.ORGANIZATION_PROFILE.build({
                          [PARAMS.ORGANIZATION_SLUG]: o.slug,
                        })}
                      >
                        <T>administration_organizations_settings</T>
                      </Button>
                    </Box>
                  </Box>
                </ListItemSecondaryAction>
              </ListItem>
            )}
          />
        </BaseView>
      </DashboardPage>
    </StyledWrapper>
  );
};
