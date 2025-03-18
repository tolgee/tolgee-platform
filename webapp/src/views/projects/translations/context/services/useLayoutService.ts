import { BASE_VIEW_PADDING } from 'tg.component/layout/BaseView';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { MENU_WIDTH } from 'tg.views/projects/projectMenu/SideMenu';

type Props = {
  sidePanelOpen: boolean;
  wider: boolean;
};

export const useLayoutService = ({ sidePanelOpen, wider }: Props) => {
  const bodyWidth = useGlobalContext((c) => c.layout.bodyWidth);
  const isSmall = bodyWidth < 800;
  const size = wider
    ? Math.min(Math.max(0.34 * bodyWidth, 400), 600)
    : Math.min(Math.max(0.24 * bodyWidth, 300), 500);
  const sidePanelWidth = sidePanelOpen && !isSmall ? size : 0;
  const rightPanelWidth = useGlobalContext((c) => c.layout.rightPanelWidth);
  const mainContentWidth =
    bodyWidth -
    MENU_WIDTH -
    rightPanelWidth -
    sidePanelWidth -
    // base view padding
    BASE_VIEW_PADDING * 2;
  return {
    sidePanelWidth,
    rightPanelWidth,
    mainContentWidth,
  };
};
