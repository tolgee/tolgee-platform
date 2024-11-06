import React from 'react';
import clsx from 'clsx';
import { Link, useLocation } from 'react-router-dom';
import { styled, Tooltip } from '@mui/material';
import { QuickStartHighlight } from 'tg.component/layout/QuickStartGuide/QuickStartHighlight';

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
    color: ${({ theme }) => theme.palette.primaryText + ' !important'};
    background: ${({ theme }) => theme.palette.grey[500] + '33 !important'};
  }
`;

type Props = {
  linkTo?: string;
  icon: React.ReactElement;
  text: string;
  selected?: boolean;
  matchAsPrefix?: boolean | string;
  hidden?: boolean;
  'data-cy': string;
  quickStart?: Omit<
    React.ComponentProps<typeof QuickStartHighlight>,
    'children'
  >;
};

export function SideMenuItem({
  linkTo,
  icon,
  text,
  selected,
  matchAsPrefix,
  hidden,
  quickStart,
  ...props
}: Props) {
  const match = useLocation();

  const isSelected = selected
    ? true
    : matchAsPrefix
    ? match.pathname.startsWith(
        typeof matchAsPrefix === 'string' ? matchAsPrefix : String(linkTo)
      )
    : match.pathname === linkTo;

  const matchesExactly = match.pathname === linkTo || selected;

  function wrapWithQuickStart(children: React.ReactNode) {
    if (quickStart) {
      return (
        <QuickStartHighlight
          {...quickStart}
          offset={-1}
          fullfiled={matchesExactly}
        >
          {children}
        </QuickStartHighlight>
      );
    } else {
      return children;
    }
  }

  return (
    <StyledItem data-cy="project-menu-item">
      {wrapWithQuickStart(
        <Tooltip
          title={text}
          placement="right"
          classes={{ tooltip: 'tooltip' }}
          disableInteractive
        >
          <Link
            data-cy={props['data-cy']}
            aria-label={text}
            to={linkTo as string}
            tabIndex={hidden ? -1 : undefined}
            className={clsx('link', { selected: isSelected })}
          >
            {icon}
          </Link>
        </Tooltip>
      )}
    </StyledItem>
  );
}
