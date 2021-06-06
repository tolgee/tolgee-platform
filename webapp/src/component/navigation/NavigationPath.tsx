import React from 'react';
import { NavigateNext } from '@material-ui/icons';
import { Link, Breadcrumbs } from '@material-ui/core';
import { Link as RouterLink } from 'react-router-dom';

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
            key={index}
            color={index === path.length - 1 ? 'primary' : 'inherit'}
            to={url}
            component={RouterLink}
          >
            {name}
          </Link>
        );
      })}
    </Breadcrumbs>
  );
};
