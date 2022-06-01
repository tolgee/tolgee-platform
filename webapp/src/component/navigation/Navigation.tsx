import React, { ReactNode } from 'react';
import { Breadcrumbs, Link, styled } from '@mui/material';
import { NavigateNext } from '@mui/icons-material';
import { Link as RouterLink } from 'react-router-dom';

const StyledLink = styled(Link)`
  display: grid;
  grid-auto-flow: column;
  align-items: center;
  gap: 8px;
`;

export type NavigationItem = [
  name: string | ReactNode,
  url?: string,
  icon?: ReactNode
];

type Props = {
  path: NavigationItem[];
};

export const Navigation: React.FC<Props> = ({ path }) => {
  return (
    <Breadcrumbs
      aria-label="breadcrumb"
      separator={<NavigateNext fontSize="small" />}
    >
      {path.map(([name, url, icon], index) => {
        return (
          <StyledLink
            key={index}
            color={index === path.length - 1 ? 'primary' : 'inherit'}
            // @ts-ignore
            to={url}
            component={RouterLink}
          >
            {icon}
            {name}
          </StyledLink>
        );
      })}
    </Breadcrumbs>
  );
};
