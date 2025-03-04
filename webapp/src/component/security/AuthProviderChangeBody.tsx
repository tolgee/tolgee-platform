import React, { FunctionComponent } from 'react';
import { Box, Paper, styled, Typography } from '@mui/material';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useIsSsoMigrationRequired } from 'tg.globalContext/helpers';

export const FULL_PAGE_BREAK_POINT = '(max-width: 700px)';

type AuthProviderDto = components['schemas']['AuthProviderDto'];

const StyledPaper = styled(Paper)`
  padding: 60px;
  display: grid;
  gap: 32px;
  background: ${({ theme }) => theme.palette.tokens.background['paper-1']};
  @media ${FULL_PAGE_BREAK_POINT} {
    padding: 10px;
    box-shadow: none;
    background: transparent;
  }
`;

type Props = {
  willBeManaged: boolean | undefined;
  authType: AuthProviderDto['authType'] | 'NONE';
  authTypeOld: AuthProviderDto['authType'] | 'NONE';
  ssoDomain: AuthProviderDto['ssoDomain'];
  children: React.ReactNode | undefined;
};

export const AuthProviderChangeBody: FunctionComponent<Props> = ({
  willBeManaged,
  authType,
  authTypeOld,
  ssoDomain,
  children,
}: Props) => {
  const isSsoMigrationRequired = useIsSsoMigrationRequired();
  const params = {
    authType,
    authTypeOld,
    ssoDomain,
    b: <b />,
    br: <br />,
  };

  let titleText: React.ReactNode | null;
  let infoText: React.ReactNode;

  switch (true) {
    case willBeManaged && isSsoMigrationRequired:
      // Migrating to SSO; migration is forced
      titleText = null;
      infoText = (
        <T
          keyName="accept_auth_provider_change_description_managed_sso"
          params={params}
        />
      );
      break;
    case willBeManaged:
      // Migrating to SSO; migration is voluntary
      titleText = null;
      infoText = (
        <T
          keyName="accept_auth_provider_change_description_managed_sso_optional"
          params={params}
        />
      );
      break;
    case authTypeOld === 'NONE':
      // Currently user has no third-party provider
      titleText = (
        <T
          keyName="accept_auth_provider_change_title_no_existing_provider"
          params={params}
        />
      );
      infoText = (
        <T
          keyName="accept_auth_provider_change_description_no_existing_provider"
          params={params}
        />
      );
      break;
    case authType === 'NONE':
      // User is removing third-party provider
      titleText = (
        <T
          keyName="accept_auth_provider_change_title_remove_existing_provider"
          params={params}
        />
      );
      infoText = (
        <T
          keyName="accept_auth_provider_change_description_remove_existing_provider"
          params={params}
        />
      );
      break;
    default:
      // From one third-party provider to another third-party provider
      titleText = (
        <T keyName="accept_auth_provider_change_title" params={params} />
      );
      infoText = (
        <T keyName="accept_auth_provider_change_description" params={params} />
      );
      break;
  }

  return (
    <StyledPaper>
      {titleText && (
        <Typography variant="h3" sx={{ textAlign: 'center' }}>
          {titleText}
        </Typography>
      )}

      <Box display="grid" gap="24px" justifyItems="center">
        <Box textAlign="center" data-cy="accept-auth-provider-change-info-text">
          {infoText}
        </Box>
        <Box display="flex" gap={3} flexWrap="wrap" justifyContent="center">
          {children}
        </Box>
      </Box>
    </StyledPaper>
  );
};
