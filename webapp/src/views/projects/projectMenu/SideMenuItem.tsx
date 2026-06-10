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
    background: transparent;
    border: 0;
    font: inherit;
    appearance: none;
    -webkit-appearance: none;
  }

  & .selected {
    color: ${({ theme }) => theme.palette.primaryText + ' !important'};
    background: ${({ theme }) => theme.palette.grey[500] + '33 !important'};
  }
`;

// Single source of truth for the side-menu item icon size, so native items and
// Tolgee app entries render a consistent icon regardless of what each passes in.
export const SIDE_MENU_ICON_SIZE = 24;

const StyledIcon = styled('span')`
  display: grid;
  place-items: center;
  width: ${SIDE_MENU_ICON_SIZE}px;
  height: ${SIDE_MENU_ICON_SIZE}px;
  font-size: ${SIDE_MENU_ICON_SIZE}px;
  line-height: 1;
  & svg {
    width: ${SIDE_MENU_ICON_SIZE}px;
    height: ${SIDE_MENU_ICON_SIZE}px;
  }
`;

type Props = {
  linkTo?: string;
  /** If provided, the item renders as a button and calls this on click. */
  onClick?: () => void;
  icon: React.ReactElement;
  text: string;
  matchAsPrefix?: boolean | string;
  hidden?: boolean;
  'data-cy': string;
  quickStart?: SideMenuItemQuickStart;
};

export type SideMenuItemQuickStart = Omit<
  React.ComponentProps<typeof QuickStartHighlight>,
  'children'
>;

export function SideMenuItem({
  linkTo,
  onClick,
  icon,
  text,
  matchAsPrefix,
  hidden,
  quickStart,
  ...props
}: Props) {
  const match = useLocation();

  const isSelected = matchAsPrefix
    ? match.pathname.startsWith(
        typeof matchAsPrefix === 'string' ? matchAsPrefix : String(linkTo)
      )
    : match.pathname === linkTo;

  const matchesExactly = match.pathname === linkTo;

  const renderedIcon = <StyledIcon>{icon}</StyledIcon>;

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
          {onClick ? (
            <button
              type="button"
              data-cy={props['data-cy']}
              aria-label={text}
              tabIndex={hidden ? -1 : undefined}
              onClick={onClick}
              className={clsx('link', { selected: isSelected })}
            >
              {renderedIcon}
            </button>
          ) : (
            <Link
              data-cy={props['data-cy']}
              aria-label={text}
              to={linkTo as string}
              tabIndex={hidden ? -1 : undefined}
              className={clsx('link', { selected: isSelected })}
            >
              {renderedIcon}
            </Link>
          )}
        </Tooltip>
      )}
    </StyledItem>
  );
}
