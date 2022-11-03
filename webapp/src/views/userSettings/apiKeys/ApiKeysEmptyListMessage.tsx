import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { Button } from '@mui/material';
import { Link } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { T } from '@tolgee/react';

export const ApiKeysEmptyListMessage = (props: { loading: boolean }) => (
  <EmptyListMessage
    loading={props.loading}
    hint={
      <Button
        component={Link}
        to={LINKS.USER_API_KEYS_GENERATE.build()}
        color="primary"
      >
        <T keyName="api-keys-empty-action">Create new Project API key</T>
      </Button>
    }
  >
    <T keyName="api-keys-empty-message">No Project API key added yet</T>
  </EmptyListMessage>
);
