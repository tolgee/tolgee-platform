import { Button, styled, Tooltip, TooltipProps } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import clsx from 'clsx';
import { useEffect, useRef, useState } from 'react';

import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { HighlightItem } from './enums';

const StyledHighlighter = styled('div')<{
  offset: number;
  borderradius: string;
}>`
  display: contents;
  & > * {
    position: relative;
  }
  & > *:after {
    content: '';
    position: absolute;
    inset: ${({ offset }) => -offset}px;
    border-radius: ${({ borderradius }) => borderradius};
    pointer-events: none;
    transition: opacity 0.5s ease-in-out, box-shadow 0.5s ease-in-out,
      transform 0.5s ease-in-out;
    opacity: 0;
  }
  & > *:before {
    content: '';
    position: absolute;
    inset: ${({ offset }) => -offset}px;
    border-radius: ${({ borderradius }) => borderradius};
    box-shadow: 0px 0px 4px 4px ${({ theme }) => theme.palette.primary.main};
    pointer-events: none;
    transition: opacity 0.5s ease-in-out, box-shadow 0.5s ease-in-out,
      transform 0.5s ease-in-out;
    opacity: 0;
  }
  &.highlight.visible > *:before {
    opacity: 0.4;
  }
  &.highlight.visible > *:after {
    opacity: 1;
  }
  &.highlight > *:after {
    border: 1px solid ${({ theme }) => theme.palette.primary.main};
  }
  &.expanded > *:before {
    box-shadow: 0px 0px 4px 8px ${({ theme }) => theme.palette.primary.main};
  }

  &.dashed.visible > *:after {
    opacity: 1;
  }
  &.dashed > *:after {
    border: 2px solid ${({ theme }) => theme.palette.primary.main};
  }
  &.dashed > *:after {
    border-style: dashed;
  }
`;

const StyledPopperContent = styled('div')`
  min-width: 100px;
  max-width: 200px;
  display: grid;
`;

const StyledPopperMessage = styled('div')`
  padding: 5px 8px;
`;

const StyledPopperActions = styled('div')`
  display: flex;
  justify-content: space-between;
`;

type Props = {
  children: React.ReactNode;
  itemKey: HighlightItem;
  message?: string;
  offset?: number;
  fullfiled?: boolean;
  disabled?: boolean;
  messagePlacement?: TooltipProps['placement'];
  borderRadius?: string;
};

export const QuickStartHighlight = ({
  children,
  itemKey,
  message,
  offset = 0,
  fullfiled,
  disabled,
  messagePlacement,
  borderRadius = 'inherit',
}: Props) => {
  const wrapperRef = useRef<HTMLDivElement>(null);
  const rightPanelWidth = useGlobalContext((c) => c.layout.rightPanelWidth);

  const itemActive = useGlobalContext(
    (c) => c.quickStartGuide.active === itemKey
  );
  const enabled = useGlobalContext((c) => c.quickStartGuide.enabled);
  const active = itemActive && enabled;
  const placement = messagePlacement ?? (rightPanelWidth ? 'right' : 'bottom');

  const { t } = useTranslate();

  const [expanded, setExpanded] = useState(false);
  const [popperOpen, setPopperOpen] = useState(false);

  useEffect(() => {
    if (active && message) {
      setPopperOpen(true);
    }
  }, [active, Boolean(message)]);

  const { quickStartVisited, quickStartSkipTips } = useGlobalActions();

  const visible = active && enabled;

  function handleCompleted() {
    setPopperOpen(false);
    quickStartVisited(itemKey);
  }

  useEffect(() => {
    if (fullfiled && active) {
      handleCompleted();
    }
  }, [fullfiled, active]);

  useEffect(() => {
    if (active && !message) {
      setExpanded(true);
      const handler = () => {
        setExpanded((val) => !val);
      };
      const timer = setInterval(handler, 500);
      return () => clearInterval(timer);
    } else {
      setExpanded(false);
    }
  }, [active, Boolean(message)]);

  function withTooltip(children: React.ReactNode) {
    if (message) {
      return (
        <Tooltip
          open={popperOpen && active}
          placement={placement}
          title={
            <StyledPopperContent>
              <StyledPopperMessage>{message}</StyledPopperMessage>
              <StyledPopperActions>
                <Button
                  size="small"
                  sx={{ padding: '0px 8px', minWidth: 40 }}
                  onClick={quickStartSkipTips}
                >
                  {t('quick_start_highlight_skip')}
                </Button>
                <Button
                  size="small"
                  color="primary"
                  sx={{ padding: '0px 8px', minWidth: 40 }}
                  onClick={handleCompleted}
                  data-cy="quick-start-highlight-ok"
                  data-cy-item={itemKey}
                >
                  {t('quick_start_highlight_ok')}
                </Button>
              </StyledPopperActions>
            </StyledPopperContent>
          }
        >
          {children as any}
        </Tooltip>
      );
    } else {
      return children;
    }
  }

  if (disabled || !visible) {
    return <>{children}</>;
  }

  return (
    <>
      <StyledHighlighter
        offset={offset}
        className={clsx({
          visible: visible,
          highlight: !message,
          expanded: !message && expanded,
          dashed: message,
        })}
        onClick={active && !message ? handleCompleted : undefined}
        ref={wrapperRef}
        borderradius={borderRadius}
        data-cy="quick-start-highlight"
        data-cy-item={itemKey}
      >
        {withTooltip(children)}
      </StyledHighlighter>
    </>
  );
};
