import { FunctionComponent } from 'react';
import { Box, Grid, Paper, Theme } from '@material-ui/core';
import { EditIconButton } from '../../../common/buttons/EditIconButton';
import { DeleteIconButton } from '../../../common/buttons/DeleteIconButton';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from '../../../../constants/links';
import { container } from 'tsyringe';
import { confirmation } from '../../../../hooks/confirmation';
import { T } from '@tolgee/react';
import makeStyles from '@material-ui/core/styles/makeStyles';
import createStyles from '@material-ui/core/styles/createStyles';
import { components } from '../../../../service/apiSchema.generated';
import { useApiMutation } from '../../../../service/http/useQueryApi';
import { MessageService } from '../../../../service/MessageService';

type ApiKeyDTO = components['schemas']['ApiKeyDTO'];

interface ApiKeysListProps {
  data: ApiKeyDTO[];
}

const messageService = container.resolve(MessageService);

const useStyles = makeStyles((theme: Theme) =>
  createStyles({
    root: {
      borderBottom: `1px solid ${theme.palette.grey.A100}`,
      '&:last-child': {
        borderBottom: `none`,
      },
    },
  })
);

const Item: FunctionComponent<{ keyDTO: ApiKeyDTO }> = (props) => {
  const classes = useStyles();

  const deleteKey = useApiMutation({
    url: '/api/apiKeys/{key}',
    method: 'delete',
    invalidatePrefix: '/api/apiKeys',
  });

  const onDelete = (dto: ApiKeyDTO) => {
    confirmation({
      title: 'Delete api key',
      message: 'Do you really want to delete api key ' + dto.key + '?',
      onConfirm: () =>
        deleteKey.mutate(
          { path: { key: dto.key! } },
          {
            onSuccess() {
              messageService.success(<T>api_key_successfully_deleted</T>);
            },
          }
        ),
    });
  };

  return (
    <Box p={2} className={classes.root}>
      <Grid container justify="space-between">
        <Grid item>
          <Box mr={2}>
            <b>
              <T>Api key list label - Api Key</T> {props.keyDTO.key}
            </b>
          </Box>
        </Grid>
        <Grid item>
          <T>Api key list label - Project</T> {props.keyDTO.projectName}
        </Grid>
      </Grid>
      <Grid container justify="space-between">
        <Grid item>
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
    </Box>
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
