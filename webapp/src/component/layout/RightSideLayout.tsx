import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { RightSidePanel } from './RightSidePanel';
import { QuickStartGuide } from './QuickStartGuide/QuickStartGuide';
import React from 'react';

type Props = {
  rightPanelContent?: (width: number) => React.ReactNode;
  hideQuickStart?: boolean;
};

export const RightSideLayout = ({
  rightPanelContent,
  hideQuickStart,
}: Props) => {
  const { setQuickStartFloatingOpen, setQuickStartOpen } = useGlobalActions();
  const rightPanelWidth = useGlobalContext((c) => c.layout.rightPanelWidth);

  const quickStartEnabled = useGlobalContext((c) => c.quickStartGuide.enabled);
  const quickStartOpen = useGlobalContext((c) => c.quickStartGuide.open);
  const quickStartFloating = useGlobalContext(
    (c) => c.layout.quickStartFloating
  );
  const quickStartFloatingOpen = useGlobalContext(
    (c) => c.quickStartGuide.floatingOpen
  );
  const aiPlaygroundEnabled = useGlobalContext((c) => c.aiPlaygroundEnabled);

  function handleClose() {
    if (quickStartFloating) {
      setQuickStartFloatingOpen(false);
    } else {
      setQuickStartOpen(false);
    }
  }

  return (
    <>
      {rightPanelContent && aiPlaygroundEnabled && (
        <RightSidePanel width={rightPanelWidth}>
          {rightPanelContent(rightPanelWidth)}
        </RightSidePanel>
      )}
      {quickStartEnabled && !hideQuickStart && (
        <RightSidePanel
          width={rightPanelWidth || 400}
          floating={quickStartFloating}
          open={quickStartFloating ? quickStartFloatingOpen : quickStartOpen}
        >
          <QuickStartGuide onClose={handleClose} />
        </RightSidePanel>
      )}
    </>
  );
};
