import { useState } from 'react';
import { Box, styled, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';

import { LINKS } from 'tg.constants/links';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { SessionListItem } from '../sessions/SessionListItem';
import { SessionsListHeader } from '../sessions/SessionsListHeader';
import { RevokeAllOtherSessionsButton } from '../sessions/RevokeAllOtherSessionsButton';

const StyledLink = styled(Link)`
  color: ${({ theme }) => theme.palette.primary.main};
  text-decoration: none;
`;

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

const StyledHeading = styled(Box)`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
`;

export const SessionsSection = () => {
  const [page, setPage] = useState(0);

  const list = useApiQuery({
    url: '/v2/user/sessions',
    method: 'get',
    query: {
      size: 20,
      page,
    },
  });

  return (
    <Box mt={4} data-cy="account-security-sessions">
      <StyledHeading>
        <Typography variant="h6">
          <T keyName="sessions_title" defaultValue="Active sessions" />
        </Typography>
        <RevokeAllOtherSessionsButton />
      </StyledHeading>

      <Box sx={{ mt: 1, mb: 2 }}>
        <T
          keyName="sessions-description"
          defaultValue="These are the devices currently signed in to your account. Revoke any session you don't recognize. Sessions don't cover <apiKeysLink>project API keys</apiKeysLink> and <patsLink>Personal Access Tokens</patsLink>, which are managed separately."
          params={{
            apiKeysLink: <StyledLink to={LINKS.USER_API_KEYS.build()} />,
            patsLink: <StyledLink to={LINKS.USER_PATS.build()} />,
          }}
        />
      </Box>

      <StyledContainer>
        <PaginatedHateoasList
          wrapperComponentProps={{ className: 'listWrapper' }}
          onPageChange={setPage}
          loadable={list}
          listComponentProps={{ subheader: <SessionsListHeader /> }}
          renderItem={(session) => <SessionListItem session={session} />}
        />
      </StyledContainer>
    </Box>
  );
};
