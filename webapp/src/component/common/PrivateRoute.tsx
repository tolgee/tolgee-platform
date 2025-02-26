import { default as React } from 'react';
import { Route } from 'react-router-dom';

import { RedirectUnsignedUser } from './RedirectUnsignedUser';

type Props = React.ComponentProps<typeof Route>;

export const PrivateRoute: React.FC<Props> = (props) => {
  return (
    <Route {...props}>
      <RedirectUnsignedUser>{props.children}</RedirectUnsignedUser>
    </Route>
  );
};
