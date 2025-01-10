import { Global, css } from '@emotion/react';
import { TOP_BAR_HEIGHT } from 'tg.component/layout/TopBar/TopBar';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

export const GlobalStyles = () => {
  const topBannerHeight = useGlobalContext((c) => c.layout.topBannerHeight);

  return (
    <Global
      styles={css`
        .SnackbarContainer-root {
          margin-top: ${topBannerHeight + TOP_BAR_HEIGHT - 12}px;
        }
      `}
    />
  );
};
