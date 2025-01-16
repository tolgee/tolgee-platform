import React from 'react';
import { styled } from '@mui/material';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

export const MENU_WIDTH = 60;

const StyledMenuWrapper = styled('div')`
  min-width: ${MENU_WIDTH}px;
`;

const StyledMenuFixed = styled('menu')`
  position: fixed;
  top: 0px;
  bottom: 0px;
  overscroll-behavior: contain;
  margin: 0px;
  padding: 0px;
  width: ${MENU_WIDTH}px;
  display: flex;
  flex-direction: column;
`;

export const SideMenu: React.FC = ({ children }) => {
  const topBannerHeight = useGlobalContext((c) => c.layout.topBannerHeight);

  return (
    <StyledMenuWrapper>
      <StyledMenuFixed
        sx={{ top: topBannerHeight }}
        color="secondary"
        data-cy="project-menu-items"
      >
        {children}
      </StyledMenuFixed>
    </StyledMenuWrapper>
  );
};
