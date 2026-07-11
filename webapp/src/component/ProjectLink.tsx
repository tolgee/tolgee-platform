import React from 'react';
import { Link } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';

type Props = {
  project: { id: number; name: string };
} & React.ComponentProps<typeof Link>;

export const ProjectLink: React.FC<React.PropsWithChildren<Props>> = ({
  project,
  children,
  ...props
}) => {
  return (
    <Link
      component={RouterLink}
      to={LINKS.PROJECT.build({ [PARAMS.PROJECT_ID]: project.id })}
      {...props}
    >
      {children ?? project.name}
    </Link>
  );
};
