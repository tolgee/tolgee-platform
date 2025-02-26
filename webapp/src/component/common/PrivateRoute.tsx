import { default as React } from 'react';
import { Route } from 'react-router-dom';

import { RedirectUnsignedUser } from './RedirectUnsignedUser';
import { RedirectWhenSsoMigrationRequired } from 'tg.component/common/RedirectWhenSsoMigrationRequired';

type Props = React.ComponentProps<typeof Route>;

export const PrivateRoute: React.FC<Props> = (props) => {
  return (
    <Route {...props}>
      <RedirectWhenSsoMigrationRequired>
        <RedirectUnsignedUser>{props.children}</RedirectUnsignedUser>
      </RedirectWhenSsoMigrationRequired>
    </Route>
  );
};
