import React from 'react';
import { styled } from '@mui/material';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

export const MENU_WIDTH = 60;
export const MENU_WIDTH_EXPANDED = 200;

const StyledMenuWrapper = styled('div')<{ expanded: boolean }>`
  min-width: ${({ expanded }) =>
    expanded ? MENU_WIDTH_EXPANDED : MENU_WIDTH}px;
  transition: min-width 0.2s ease-in-out;
`;

const StyledMenuFixed = styled('menu')<{ expanded: boolean }>`
  position: fixed;
  top: 0px;
  bottom: 0px;
  overscroll-behavior: contain;
  margin: 0px;
  padding: 0px;
  width: ${({ expanded }) => (expanded ? MENU_WIDTH_EXPANDED : MENU_WIDTH)}px;
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease-in-out;
`;

export const SideMenu: React.FC<{
  expanded: boolean;
  children: React.ReactNode;
}> = ({ expanded, children }) => {
  const topBannerHeight = useGlobalContext((c) => c.layout.topBannerHeight);

  return (
    <StyledMenuWrapper expanded={expanded}>
      <StyledMenuFixed
        expanded={expanded}
        sx={{ top: topBannerHeight }}
        color="secondary"
        data-cy="project-menu-items"
      >
        {children}
      </StyledMenuFixed>
    </StyledMenuWrapper>
  );
};
