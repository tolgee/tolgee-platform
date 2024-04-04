import { styled } from '@mui/material';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

const StyledAppBarSpacer = styled('div')(
  ({ theme }) => theme.mixins.toolbar as any
);

export function TopSpacer() {
  const topBannerHeight = useGlobalContext((c) => c.layout.topBannerHeight);

  return <StyledAppBarSpacer sx={{ marginTop: topBannerHeight + 'px' }} />;
}
