import { useState } from 'react';
import { Box, Button, styled, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';

import { LINKS } from 'tg.constants/links';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { SessionListItem } from './SessionListItem';
import { SessionsListHeader } from './SessionsListHeader';
import { RevokeAllOtherSessionsButton } from './RevokeAllOtherSessionsButton';

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
  // Listing requires super authentication, and fetching on mount would put a password prompt in
  // front of anyone who opened Account Security to do something else entirely.
  const [revealed, setRevealed] = useState(false);

  const list = useApiQuery({
    url: '/v2/user/sessions',
    method: 'get',
    query: {
      size: 20,
      page,
    },
    options: {
      enabled: revealed,
    },
  });

  return (
    <Box mt={4} data-cy="account-security-sessions">
      <StyledHeading>
        <Typography variant="h6">
          <T keyName="sessions-title" defaultValue="Active sessions" />
        </Typography>
        {revealed && <RevokeAllOtherSessionsButton />}
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

      {!revealed ? (
        <Button
          data-cy="sessions-reveal-button"
          variant="outlined"
          onClick={() => setRevealed(true)}
        >
          <T
            keyName="sessions-reveal-button"
            defaultValue="Show active sessions"
          />
        </Button>
      ) : (
        <StyledContainer>
          <PaginatedHateoasList
            wrapperComponentProps={{ className: 'listWrapper' }}
            onPageChange={setPage}
            loadable={list}
            listComponentProps={{ subheader: <SessionsListHeader /> }}
            renderItem={(session) => <SessionListItem session={session} />}
          />
        </StyledContainer>
      )}
    </Box>
  );
};
