import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useHistory } from 'react-router-dom';
import { Button } from '@mui/material';
import { LINKS } from 'tg.constants/links';
import { T } from '@tolgee/react';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';

export const DebugCustomerAccountButton = (props: { userId: number }) => {
  const debugAccount = useApiMutation({
    url: '/v2/administration/users/{userId}/generate-token',
    method: 'get',
  });

  const { debugCustomerAccount } = useGlobalActions();

  const history = useHistory();

  return (
    <Button
      data-cy="administration-user-debug-account"
      size="small"
      variant="contained"
      color="error"
      onClick={() => {
        debugAccount.mutate(
          { path: { userId: props.userId } },
          {
            onSuccess: (r) => {
              debugCustomerAccount(r);
              history.push(LINKS.PROJECTS.build());
            },
          }
        );
      }}
    >
      <T keyName="administration_user_debug" />
    </Button>
  );
};
