import { FunctionComponent } from 'react';
import { Box, Grid, Paper, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';
import { container } from 'tsyringe';

import { DeleteIconButton } from 'tg.component/common/buttons/DeleteIconButton';
import { EditIconButton } from 'tg.component/common/buttons/EditIconButton';
import { LINKS, PARAMS } from 'tg.constants/links';
import { confirmation } from 'tg.hooks/confirmation';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';

type ApiKeyModel = components['schemas']['ApiKeyModel'];

interface ApiKeysListProps {
  data: ApiKeyModel[];
}

const messageService = container.resolve(MessageService);

const StyledRoot = styled(Box)`
  border-bottom: 1px solid ${({ theme }) => theme.palette.emphasis.A100};
  &:last-child {
    border-bottom: none;
  }
`;

const Item: FunctionComponent<{ keyDTO: ApiKeyModel }> = (props) => {
  const deleteKey = useApiMutation({
    url: '/v2/api-keys/{apiKeyId}',
    method: 'delete',
    invalidatePrefix: '/v2/api-keys',
  });

  const onDelete = (dto: components['schemas']['ApiKeyModel']) => {
    confirmation({
      title: <T keyName="delete_api_key">Delete api key</T>,
      message: (
        <span data-openreplay-masked="">
          <T
            keyName="really_delete_api_key_message"
            parameters={{ key: dto.key }}
            //eslint-disable-next-line react/no-children-prop
            children="Do you really want to delete api key {key}?"
          />
        </span>
      ),
      onConfirm: () =>
        deleteKey.mutate(
          { path: { apiKeyId: dto.id } },
          {
            onSuccess() {
              messageService.success(<T>api_key_successfully_deleted</T>);
            },
          }
        ),
    });
  };

  return (
    <StyledRoot p={2}>
      <Grid container justifyContent="space-between">
        <Grid item>
          <Box mr={2}>
            <b>
              <T>Api key list label - Api Key</T>{' '}
              <span data-openreplay-masked="">{props.keyDTO.key}</span>
            </b>
          </Box>
        </Grid>
        <Grid item>
          <T>Api key list label - Project</T> {props.keyDTO.projectName}
        </Grid>
      </Grid>
      <Grid container justifyContent="space-between">
        <Grid item lg={10} md={9} sm={8} xs={6}>
          <T>Api key list label - Scopes</T>&nbsp;
          {props.keyDTO.scopes?.join(', ')}
        </Grid>
        <Grid item>
          <EditIconButton
            data-cy="api-keys-edit-button"
            component={Link}
            to={LINKS.USER_API_KEYS_EDIT.build({
              [PARAMS.API_KEY_ID]: props.keyDTO.id!,
            })}
            size="small"
          />
          <DeleteIconButton
            onClick={() => onDelete(props.keyDTO)}
            size="small"
          />
        </Grid>
      </Grid>
    </StyledRoot>
  );
};

export const ApiKeysList: FunctionComponent<ApiKeysListProps> = (props) => {
  return (
    <Paper elevation={0} variant={'outlined'}>
      {props.data.map((k) => (
        <Item keyDTO={k} key={k.id!.toString()} />
      ))}
    </Paper>
  );
};
