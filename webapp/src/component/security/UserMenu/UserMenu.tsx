import React from 'react';

import {
  useConfig,
  useIsEmailVerified,
  useUser,
} from 'tg.globalContext/helpers';
import { UserMissingAvatarMenu } from './UserMissingAvatarMenu';
import { UserPresentAvatarMenu } from './UserPresentAvatarMenu';
import { UserUnverifiedEmailMenu } from './UserUnverifiedEmailMenu';

export const UserMenu: React.FC = () => {
  const config = useConfig();
  const user = useUser();
  const isEmailVerified = useIsEmailVerified();

  if (!isEmailVerified) {
    return <UserUnverifiedEmailMenu />;
  }

  if (!config.authentication || !user) {
    return <UserMissingAvatarMenu />;
  } else {
    return <UserPresentAvatarMenu />;
  }
};
