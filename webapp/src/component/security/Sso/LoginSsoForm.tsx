import React, {RefObject} from 'react';
import {Link as MuiLink, styled, Typography} from '@mui/material';
import Box from '@mui/material/Box';
import {T} from '@tolgee/react';
import {Link} from 'react-router-dom';

import {LINKS} from 'tg.constants/links';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import {StandardForm} from 'tg.component/common/form/StandardForm';
import {TextField} from 'tg.component/common/form/fields/TextField';
import {useGlobalActions, useGlobalContext,} from 'tg.globalContext/GlobalContext';
import {v4 as uuidv4} from 'uuid';

const StyledInputFields = styled('div')`
  display: grid;
  align-items: start;
  gap: 16px;
  padding-bottom: 32px;
`;

type Credentials = { domain: string };
type LoginViewCredentialsProps = {
  credentialsRef: RefObject<Credentials>;
};
const LOCAL_STORAGE_STATE_KEY = 'oauth2State';

export function LoginSsoForm(props: LoginViewCredentialsProps) {
  const { getSsoAuthLinkByDomain } = useGlobalActions();
  const isLoading = useGlobalContext((c) => c.auth.loginLoadable.isLoading);

  return (
    <StandardForm
      initialValues={props.credentialsRef.current!}
      submitButtons={
        <Box>
          <Box display="flex" flexDirection="column" alignItems="stretch">
            <LoadingButton
              loading={isLoading}
              variant="contained"
              color="primary"
              type="submit"
              data-cy="login-button"
            >
              <T keyName="login_login_button" />
            </LoadingButton>

            <Box display="flex" justifyContent="center" flexWrap="wrap" mt={1}>
              <MuiLink to={LINKS.LOGIN.build()} component={Link}>
                <Typography variant="body2">
                  <T keyName="login_sso_more_ways_to_login" />
                </Typography>
              </MuiLink>
            </Box>
          </Box>
        </Box>
      }
      onSubmit={async (data) => {
        const state = uuidv4();
        localStorage.setItem(LOCAL_STORAGE_STATE_KEY, state);
        const response = await getSsoAuthLinkByDomain(data.domain, state);
        window.location.href = response.redirectUrl;
      }}
    >
      <StyledInputFields>
        <TextField
          name="domain"
          label={<T keyName="login_sso_domain" />}
          minHeight={false}
        />
      </StyledInputFields>
    </StandardForm>
  );
}
