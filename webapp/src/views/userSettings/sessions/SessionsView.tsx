import { useState } from 'react';
import { Box, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';

import { LINKS } from 'tg.constants/links';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { BaseUserSettingsView } from '../BaseUserSettingsView';
import { RevokeAllOtherSessionsButton } from './RevokeAllOtherSessionsButton';
import { SessionListItem } from './SessionListItem';

const StyledLink = styled(Link)`
  color: ${({ theme }) => theme.palette.primary.main};
  text-decoration: none;
`;

export const SessionsView = () => {
  const { t } = useTranslate();
  const [page, setPage] = useState(0);

  const list = useApiQuery({
    url: '/v2/user/sessions',
    method: 'get',
    query: {
      size: 50,
      page,
    },
  });

  return (
    <BaseUserSettingsView
      windowTitle={t('sessions_title', 'Active sessions')}
      title={t('sessions_title', 'Active sessions')}
      loading={list.isFetching}
      navigation={[
        [
          t('user_menu_sessions', 'Active sessions'),
          LINKS.USER_SESSIONS.build(),
        ],
      ]}
      hideChildrenOnLoading={false}
    >
      <Box sx={{ my: 2 }}>
        <T
          keyName="sessions-description"
          defaultValue="These are the devices currently signed in to your account. Revoke any session you don't recognize. Sessions don't cover <apiKeysLink>project API keys</apiKeysLink> and <patsLink>Personal Access Tokens</patsLink>, which are managed separately."
          params={{
            apiKeysLink: <StyledLink to={LINKS.USER_API_KEYS.build()} />,
            patsLink: <StyledLink to={LINKS.USER_PATS.build()} />,
          }}
        />
      </Box>
      <Box sx={{ mb: 2 }}>
        <RevokeAllOtherSessionsButton />
      </Box>
      <PaginatedHateoasList
        wrapperComponentProps={{ className: 'listWrapper' }}
        onPageChange={setPage}
        loadable={list}
        renderItem={(session) => <SessionListItem session={session} />}
      />
    </BaseUserSettingsView>
  );
};
