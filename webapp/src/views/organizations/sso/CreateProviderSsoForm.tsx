import React from 'react';
import { styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { Validation } from 'tg.constants/GlobalValidationSchema';

const StyledInputFields = styled('div')`
  display: grid;
  align-items: start;
  gap: 16px;
  padding-bottom: 32px;
`;

type FormValues = {
  authorizationUri: string;
  clientId: string;
  clientSecret: string;
  tokenUri: string;
  // jwkSetUri: string;
  domain: string;
};

export function CreateProviderSsoForm({ data, disabled }) {
  const organization = useOrganization();
  const { t } = useTranslate();
  const initialValues: FormValues = {
    authorizationUri: data?.authorizationUri ?? '',
    clientId: data?.clientId ?? '',
    clientSecret: data?.clientSecret ?? '',
    tokenUri: data?.tokenUri ?? '',
    // jwkSetUri: data?.jwkSetUri ?? '',
    domain: data?.domain ?? '',
  };

  if (!organization) {
    return null;
  }

  const providersCreate = useApiMutation({
    url: `/v2/organizations/{organizationId}/sso`,
    method: 'put',
    invalidatePrefix: '/v2/organizations',
  });

  return (
    <StandardForm
      initialValues={initialValues}
      validationSchema={Validation.SSO_PROVIDER(t)}
      onSubmit={async (data) => {
        providersCreate.mutate(
          {
            path: { organizationId: organization.id },
            content: { 'application/json': { ...data, enabled: !disabled } },
          },
          {
            onSuccess(data) {
              messageService.success(
                <T keyName="organization_add_provider_success_message" />
              );
            },
          }
        );
      }}
    >
      <StyledInputFields>
        <TextField
          disabled={disabled}
          variant="standard"
          name="domain"
          label={<T keyName="organization_sso_domain_name" />}
          minHeight={false}
          helperText={<T keyName="sso_domain_name_helper_text" />}
        />
      </StyledInputFields>
      <StyledInputFields>
        <TextField
          disabled={disabled}
          variant="standard"
          name="authorizationUri"
          label={<T keyName="organization_sso_authorization_uri" />}
          minHeight={false}
          helperText={<T keyName="sso_auth_uri_helper_text" />}
        />
      </StyledInputFields>
      <StyledInputFields>
        <TextField
          disabled={disabled}
          variant="standard"
          name="clientId"
          label={<T keyName="organization_sso_client_id" />}
          minHeight={false}
          helperText={<T keyName="sso_client_id_helper_text" />}
        />
      </StyledInputFields>
      <StyledInputFields>
        <TextField
          disabled={disabled}
          variant="standard"
          name="clientSecret"
          label={<T keyName="organization_sso_client_secret" />}
          minHeight={false}
          helperText={<T keyName="sso_client_secret_helper_text" />}
        />
      </StyledInputFields>
      <StyledInputFields>
        <TextField
          disabled={disabled}
          variant="standard"
          name="tokenUri"
          label={<T keyName="organization_sso_token_uri" />}
          minHeight={false}
          helperText={<T keyName="sso_token_uri_helper_text" />}
        />
      </StyledInputFields>
      {/*<StyledInputFields>*/}
      {/*  <TextField*/}
      {/*    disabled={disabled}*/}
      {/*    variant="standard"*/}
      {/*    name="jwkSetUri"*/}
      {/*    label={<T keyName="organization_sso_jwk_set_uri" />}*/}
      {/*    minHeight={false}*/}
      {/*    helperText={<T keyName="sso_jwk_set_uri_helper_text" />}*/}
      {/*  />*/}
      {/*</StyledInputFields>*/}
    </StandardForm>
  );
}
