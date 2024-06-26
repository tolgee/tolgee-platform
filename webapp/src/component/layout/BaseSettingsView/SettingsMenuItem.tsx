import React from 'react';
import clsx from 'clsx';
import { Link, useLocation } from 'react-router-dom';
import { styled } from '@mui/material';

const StyledItem = styled('div')`
  display: flex;
  flex-direction: column;

  & .link {
    display: flex;
    padding: 6px 12px;
    cursor: pointer;
    color: ${({ theme }) => theme.palette.tokens.text.primary};
    outline: 0;
    transition: all 0.2s ease-in-out;
    text-decoration: none;
    &:focus,
    &:hover {
      color: ${({ theme }) => theme.palette.emphasis[800]};
    }
    border-radius: 16px;
  }

  & .selected {
    color: ${({ theme }) => theme.palette.primary.main + ' !important'};
    background: ${({ theme }) => theme.palette.grey[500] + '33 !important'};
  }
`;

type Props = {
  linkTo?: string;
  text: string;
  selected?: boolean;
  matchAsPrefix?: boolean;
  hidden?: boolean;
};

export const SettingsMenuItem: React.FC<Props> = ({
  linkTo,
  text,
  selected,
  matchAsPrefix,
  hidden,
}) => {
  const match = useLocation();

  const isSelected = selected
    ? true
    : matchAsPrefix
    ? match.pathname.startsWith(String(linkTo))
    : match.pathname === linkTo;

  return (
    <StyledItem>
      <Link
        aria-label={text}
        to={linkTo as string}
        data-cy="settings-menu-item"
        tabIndex={hidden ? -1 : undefined}
        className={clsx('link', { selected: isSelected })}
      >
        {text}
      </Link>
    </StyledItem>
  );
};
