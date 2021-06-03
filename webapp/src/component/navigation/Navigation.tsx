import React from 'react';
import { NavigationWrapper } from './NavigationWrapper';
import { NavigationPath } from './NavigationPath';

type Props = {
  path: React.ComponentProps<typeof NavigationPath>['path'];
  rightContent?: React.ReactNode;
};

export const Navigation: React.FC<Props> = ({ path, rightContent }) => {
  return (
    <NavigationWrapper>
      <NavigationPath path={path} />
      {rightContent}
    </NavigationWrapper>
  );
};
