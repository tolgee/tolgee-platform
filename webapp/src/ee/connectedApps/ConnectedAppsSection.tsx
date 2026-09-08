import { useState } from 'react';
import { Box, styled, Typography } from '@mui/material';
import { T } from '@tolgee/react';

import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { ConnectedAppItem } from './ConnectedAppItem';
import { ConnectedAppsListHeader } from './ConnectedAppsListHeader';

/**
 * The rows switch between a table and a stacked card on the width of the list itself, which in a
 * settings page is far narrower than the viewport - so the container context has to start here.
 */
const StyledContainer = styled(Box)`
  container-type: inline-size;

  & .listWrapper ul {
    padding: 0;
  }
`;

export const ConnectedAppsSection = () => {
  const [page, setPage] = useState(0);

  const list = useApiQuery({
    url: '/v2/user/connected-apps',
    method: 'get',
    query: { size: 20, page },
  });

  const total = list.data?.page?.totalElements ?? 0;
  if (list.isSuccess && total === 0) {
    return null;
  }

  return (
    <Box mt={4} data-cy="account-security-connected-apps">
      <Typography variant="h6">
        <T keyName="connected-apps-title" defaultValue="Connected apps" />
      </Typography>

      <Box sx={{ mt: 1, mb: 2 }}>
        <T
          keyName="connected-apps-description"
          defaultValue="Applications you authorized to access your account on your behalf. Disconnect any you no longer use."
        />
      </Box>

      <StyledContainer>
        <PaginatedHateoasList
          wrapperComponentProps={{ className: 'listWrapper' }}
          onPageChange={setPage}
          loadable={list}
          listComponentProps={{ subheader: <ConnectedAppsListHeader /> }}
          renderItem={(app) => <ConnectedAppItem app={app} />}
        />
      </StyledContainer>
    </Box>
  );
};
