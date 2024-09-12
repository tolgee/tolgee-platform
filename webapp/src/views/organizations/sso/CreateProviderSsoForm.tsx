import React, { RefObject } from 'react';
import { styled } from '@mui/material';
import { T } from '@tolgee/react';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';

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
    credentialsRef: RefObject<Provider>;
};

export function CreateProviderSsoForm(props: ProviderProps) {

    const providersLoadable = useApiMutation({
        url: `/v2/sso/providers`,
        method: 'post',
    });


    return (
        <StandardForm
            initialValues={props.credentialsRef.current!}

            onSubmit={async (data) => {
                providersLoadable.mutate(
                    {
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
                    variant="standard"
                    name="authorizationUri"
                    label={<T keyName="organization_sso_authorization_uri"/>}
                    minHeight={false}
                />
            </StyledInputFields>
            <StyledInputFields>
                <TextField
                    variant="standard"
                    name="clientId"
                    label={<T keyName="organization_sso_client_id"/>}
                    minHeight={false}
                />
            </StyledInputFields>
            <StyledInputFields>
                <TextField
                    variant="standard"
                    name="clientSecret"
                    label={<T keyName="organization_sso_client_secret"/>}
                    minHeight={false}
                />
            </StyledInputFields>
            <StyledInputFields>
                <TextField
                    variant="standard"
                    name="redirectUri"
                    label={<T keyName="organization_sso_redirect_uri"/>}
                    minHeight={false}
                />
            </StyledInputFields>
            <StyledInputFields>
                <TextField
                    variant="standard"
                    name="tokenUri"
                    label={<T keyName="organization_sso_token_uri"/>}
                    minHeight={false}
                />
            </StyledInputFields>
        </StandardForm>
    );
}
