import React from 'react';
import { Link, Breadcrumbs } from '@material-ui/core';

type Props = {
  path: [name: string, url: string][];
};

export const Path: React.FC<Props> = ({ path }) => {
  return (
    <Breadcrumbs aria-label="breadcrumb">
      {path.map(([name, url], index) => {
        return <Link href={url}>{name}</Link>;
      })}
    </Breadcrumbs>
  );
};
