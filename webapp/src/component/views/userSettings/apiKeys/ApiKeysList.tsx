import {default as React, FunctionComponent} from 'react';
import {ApiKeyDTO} from '../../../../service/response.types';
import {Box, Grid, Paper, Theme,} from '@material-ui/core';
import {EditIconButton} from '../../../common/buttons/EditIconButton';
import {DeleteIconButton} from '../../../common/buttons/DeleteIconButton';
import {Link} from 'react-router-dom';
import {LINKS, PARAMS} from '../../../../constants/links';
import {container} from 'tsyringe';
import {UserApiKeysActions} from '../../../../store/api_keys/UserApiKeysActions';
import {confirmation} from '../../../../hooks/confirmation';
import {T} from '@tolgee/react';
import makeStyles from '@material-ui/core/styles/makeStyles';
import createStyles from '@material-ui/core/styles/createStyles';

interface ApiKeysListProps {
  data: ApiKeyDTO[];
}

const actions = container.resolve(UserApiKeysActions);

const onDelete = (dto: ApiKeyDTO) => {
  const onConfirm = () => actions.loadableActions.delete.dispatch(dto.key);
  confirmation({
    title: 'Delete api key',
    message: 'Do you really want to delete api key ' + dto.key + '?',
    onConfirm,
  });
};

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
          {props.keyDTO.scopes.join(', ')}
        </Grid>
        <Grid item>
          <EditIconButton
            data-cy="api-keys-edit-button"
            component={Link}
            to={LINKS.USER_API_KEYS_EDIT.build({
              [PARAMS.API_KEY_ID]: props.keyDTO.id,
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
        <Item keyDTO={k} key={k.id.toString()} />
      ))}
    </Paper>
  );
};
