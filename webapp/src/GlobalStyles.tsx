import { css, GlobalStyles as Global, useTheme } from '@mui/material';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

export const GlobalStyles = () => {
  const topBannerHeight = useGlobalContext((c) => c.layout.topBannerHeight);
  const theme = useTheme();
  const toolbarMinHeight = theme.mixins.toolbar.minHeight as number;

  return (
    <Global
      styles={css`
        .notistack-SnackbarContainer {
          margin-top: ${topBannerHeight + toolbarMinHeight - 12}px;
        }
        .notistack-Snackbar .notistack-CloseButton {
          opacity: 0;
          transition: opacity 0.1s ease-in-out;
        }
        .notistack-Snackbar:hover .notistack-CloseButton {
          opacity: 1;
        }
      `}
    />
  );
};
