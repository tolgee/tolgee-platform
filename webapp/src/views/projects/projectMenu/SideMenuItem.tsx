import React from 'react';
import clsx from 'clsx';
import { Link, useLocation } from 'react-router-dom';
import { styled, Tooltip } from '@mui/material';

type SideMenuItemProps = {
  linkTo?: string;
  icon: React.ReactElement;
  text: string;
  selected?: boolean;
  matchAsPrefix?: boolean;
  hidden?: boolean;
};

const StyledItem = styled('li')`
  display: flex;
  list-style: none;
  flex-direction: column;
  align-items: center;

  & .tooltip {
    margin: ${({ theme }) => theme.spacing(0, 0.5)};
  }

  & .link {
    display: flex;
    padding: 10px 0px;
    cursor: pointer;
    justify-content: center;
    color: ${({ theme }) => theme.palette.emphasis[600]};
    outline: 0;
    transition: all 0.2s ease-in-out;
    &:focus,
    &:hover {
      color: ${({ theme }) => theme.palette.emphasis[800]};
    }
    width: 44px;
    border-radius: 10px;
  }

  & .selected {
    color: ${({ theme }) => theme.palette.primary.main + ' !important'};
    background: ${({ theme }) => theme.palette.grey[500] + '33 !important'};
  }
`;

export function SideMenuItem({
  linkTo,
  icon,
  text,
  selected,
  matchAsPrefix,
  hidden,
}: SideMenuItemProps) {
  const match = useLocation();

  const isSelected = selected
    ? true
    : matchAsPrefix
    ? match.pathname.startsWith(String(linkTo))
    : match.pathname === linkTo;

  return (
    <StyledItem>
      <Tooltip title={text} placement="right" classes={{ tooltip: 'tooltip' }}>
        <Link
          aria-label={text}
          to={linkTo as string}
          tabIndex={hidden ? -1 : undefined}
          className={clsx('link', { selected: isSelected })}
        >
          {icon}
        </Link>
      </Tooltip>
    </StyledItem>
  );
}
