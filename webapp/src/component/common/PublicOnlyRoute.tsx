import { default as React } from 'react';
import { Route } from 'react-router-dom';

import { RedirectSignedUser } from './RedirectSignedUser';

type Props = React.ComponentProps<typeof Route>;

export const PublicOnlyRoute: React.FC<Props> = (props) => {
  return (
    <Route {...props}>
      <RedirectSignedUser>{props.children}</RedirectSignedUser>
    </Route>
  );
};
