import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { Button } from '@mui/material';
import { Link } from 'react-router-dom';
import { LINKS } from 'tg.constants/links';
import { T } from '@tolgee/react';

export const PatEmptyListMessage = (props: { loading: boolean }) => (
  <EmptyListMessage
    loading={props.loading}
    hint={
      <Button
        component={Link}
        to={LINKS.USER_PATS_GENERATE.build()}
        color="primary"
      >
        <T keyName="pats-empty-action">Create new token</T>
      </Button>
    }
  >
    <T keyName="pats-empty-message">No Personal Access Tokens added yet.</T>
  </EmptyListMessage>
);
