import React from 'react';

import { useConfig, useUser } from 'tg.globalContext/helpers';
import { UserMissingMenu } from './UserMissingMenu';
import { UserPresentMenu } from './UserPresentMenu';

export const UserMenu: React.FC = () => {
  const config = useConfig();
  const user = useUser();

  if (!config.authentication || !user) {
    return <UserMissingMenu />;
  } else {
    return <UserPresentMenu />;
  }
};
