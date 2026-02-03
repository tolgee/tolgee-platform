import React from 'react';
import clsx from 'clsx';
import { Link, useLocation } from 'react-router-dom';
import { styled, Tooltip } from '@mui/material';
import { QuickStartHighlight } from 'tg.component/layout/QuickStartGuide/QuickStartHighlight';

const StyledItem = styled('li')<{ expanded: boolean }>`
  display: flex;
  list-style: none;
  flex-direction: column;
  align-items: center;

  & .tooltip {
    margin: ${({ theme }) => theme.spacing(0, 0.5)};
  }

  & .link {
    display: flex;
    padding: 10px ${({ expanded }) => (expanded ? '16px' : '0px')};
    cursor: pointer;
    justify-content: ${({ expanded }) => (expanded ? 'flex-start' : 'center')};
    align-items: center;
    gap: ${({ expanded }) => (expanded ? '12px' : '0px')};
    color: ${({ theme }) => theme.palette.emphasis[600]};
    outline: 0;
    transition: all 0.2s ease-in-out;
    &:focus,
    &:hover {
      color: ${({ theme }) => theme.palette.emphasis[800]};
    }
    width: ${({ expanded }) => (expanded ? 'calc(100% - 8px)' : '44px')};
    margin: ${({ expanded }) => (expanded ? '0 4px' : '0')};
    border-radius: 10px;
    white-space: nowrap;
    overflow: hidden;
    text-decoration: none;
  }

  & .selected {
    color: ${({ theme }) => theme.palette.primaryText + ' !important'};
    background: ${({ theme }) => theme.palette.grey[500] + '33 !important'};
  }

  & .text {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 14px;
  }
`;

type Props = {
  linkTo?: string;
  icon: React.ReactElement;
  text: string;
  matchAsPrefix?: boolean | string;
  hidden?: boolean;
  'data-cy': string;
  quickStart?: SideMenuItemQuickStart;
  expanded?: boolean;
};

export type SideMenuItemQuickStart = Omit<
  React.ComponentProps<typeof QuickStartHighlight>,
  'children'
>;

export function SideMenuItem({
  linkTo,
  icon,
  text,
  matchAsPrefix,
  hidden,
  quickStart,
  expanded = false,
  ...props
}: Props) {
  const match = useLocation();

  const isSelected = matchAsPrefix
    ? match.pathname.startsWith(
        typeof matchAsPrefix === 'string' ? matchAsPrefix : String(linkTo)
      )
    : match.pathname === linkTo;

  const matchesExactly = match.pathname === linkTo;

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
    <StyledItem data-cy="project-menu-item" expanded={expanded}>
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
            {expanded && <span className="text">{text}</span>}
          </Link>
        </Tooltip>
      )}
    </StyledItem>
  );
}
