import React from 'react';
import { styled } from '@mui/material';
import { T } from '@tolgee/react';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { useOrganization } from 'tg.views/organizations/useOrganization';

const StyledInputFields = styled('div')`
  display: grid;
  align-items: start;
  gap: 16px;
  padding-bottom: 32px;
`;

type Provider = {
    authorizationUri: string;
    clientId: string;
    clientSecret: string;
    redirectUri: string;
    tokenUri: string;
};
type ProviderProps = {
  credentialsRef: React.RefObject<Provider>;
};

export function CreateProviderSsoForm({ credentialsRef, disabled }) {

    const organization = useOrganization();
    if (!organization) {
        return null;
    }

    const providersCreate = useApiMutation({
        url: `/v2/{organizationId}/sso/providers`,
        method: 'post',
        invalidatePrefix: '/v2/organizations',
    })

    return (
        <StandardForm
            initialValues={credentialsRef.current!}
            onSubmit={async (data) => {
                providersCreate.mutate(
                    {
                        path: {organizationId: organization.id},
                        content: {'application/json': {...data}},
                    },
                    {
                        onSuccess(data) {
                            messageService.success(<T keyName="organization_add_provider_success_message"/>);
                        }
                    }
            ,
            )
                ;
            }}
        >
            <StyledInputFields>
                <TextField
                    disabled={disabled}
                    variant="standard"
                    name="authorizationUri"
                    label={<T keyName="organization_sso_authorization_uri"/>}
                    minHeight={false}
                />
            </StyledInputFields>
            <StyledInputFields>
                <TextField
                    disabled={disabled}
                    variant="standard"
                    name="clientId"
                    label={<T keyName="organization_sso_client_id"/>}
                    minHeight={false}
                />
            </StyledInputFields>
            <StyledInputFields>
                <TextField
                    disabled={disabled}
                    variant="standard"
                    name="clientSecret"
                    label={<T keyName="organization_sso_client_secret"/>}
                    minHeight={false}
                />
            </StyledInputFields>
            <StyledInputFields>
                <TextField
                    disabled={disabled}
                    variant="standard"
                    name="redirectUri"
                    label={<T keyName="organization_sso_redirect_uri"/>}
                    minHeight={false}
                />
            </StyledInputFields>
            <StyledInputFields>
                <TextField
                    disabled={disabled}
                    variant="standard"
                    name="tokenUri"
                    label={<T keyName="organization_sso_token_uri"/>}
                    minHeight={false}
                />
            </StyledInputFields>
        </StandardForm>
    );
}
