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

type InnerProps = {
  willBeManaged: boolean | undefined;
  authType: AuthProviderDto['authType'] | 'NONE';
  authTypeOld: AuthProviderDto['authType'] | 'NONE';
  ssoDomain: AuthProviderDto['ssoDomain'];
};

type Props = InnerProps & {
  children: React.ReactNode | undefined;
};

function resolveTitleAndText(
  { willBeManaged, authType, authTypeOld, ssoDomain }: InnerProps,
  isSsoMigrationRequired: boolean | undefined
) {
  const params = {
    authType,
    authTypeOld,
    ssoDomain,
    b: <b />,
    br: <br />,
  };

  switch (true) {
    case willBeManaged && isSsoMigrationRequired:
      // Migrating to SSO; migration is forced
      return {
        title: null,
        text: (
          <T
            keyName="accept_auth_provider_change_description_managed_sso"
            params={params}
          />
        ),
      } as const;
    case willBeManaged:
      // Migrating to SSO; migration is voluntary
      return {
        title: null,
        text: (
          <T
            keyName="accept_auth_provider_change_description_managed_sso_optional"
            params={params}
          />
        ),
      } as const;
    case authTypeOld === 'NONE':
      // Currently user has no third-party provider
      return {
        title: (
          <T
            keyName="accept_auth_provider_change_title_no_existing_provider"
            params={params}
          />
        ),
        text: (
          <T
            keyName="accept_auth_provider_change_description_no_existing_provider"
            params={params}
          />
        ),
      } as const;
    case authType === 'NONE':
      // User is removing third-party provider
      return {
        title: (
          <T
            keyName="accept_auth_provider_change_title_remove_existing_provider"
            params={params}
          />
        ),
        text: (
          <T
            keyName="accept_auth_provider_change_description_remove_existing_provider"
            params={params}
          />
        ),
      } as const;
    default:
      // From one third-party provider to another third-party provider
      return {
        title: (
          <T keyName="accept_auth_provider_change_title" params={params} />
        ),
        text: (
          <T
            keyName="accept_auth_provider_change_description"
            params={params}
          />
        ),
      } as const;
  }
}

export const AuthProviderChangeBody: FunctionComponent<Props> = ({
  children,
  ...props
}: Props) => {
  const isSsoMigrationRequired = useIsSsoMigrationRequired();
  const { title, text } = resolveTitleAndText(props, isSsoMigrationRequired);

  return (
    <StyledPaper>
      {title && (
        <Typography variant="h3" sx={{ textAlign: 'center' }}>
          {title}
        </Typography>
      )}

      <Box display="grid" gap="24px" justifyItems="center">
        <Box textAlign="center" data-cy="accept-auth-provider-change-info-text">
          {text}
        </Box>
        <Box display="flex" gap={3} flexWrap="wrap" justifyContent="center">
          {children}
        </Box>
      </Box>
    </StyledPaper>
  );
};
