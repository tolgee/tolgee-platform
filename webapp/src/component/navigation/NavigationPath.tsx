import React from 'react';
import { NavigateNext } from '@material-ui/icons';
import { Link, Breadcrumbs } from '@material-ui/core';

type Props = {
  path: [name: string, url: string][];
};

export const NavigationPath: React.FC<Props> = ({ path }) => {
  return (
    <Breadcrumbs
      aria-label="breadcrumb"
      separator={<NavigateNext fontSize="small" />}
    >
      {path.map(([name, url], index) => {
        return (
          <Link
            color={index === path.length - 1 ? 'primary' : 'inherit'}
            href={url}
          >
            {name}
          </Link>
        );
      })}
    </Breadcrumbs>
  );
};
