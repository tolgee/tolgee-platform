import { useEffect, useRef } from 'react';
import { QuickStartHighlight } from './QuickStartHighlight';
import { Box, Portal } from '@mui/material';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';

type Props = Omit<
  React.ComponentProps<typeof QuickStartHighlight>,
  'children'
> & {
  elId: string;
};

export const QuickStartHighlightInPortal = ({ elId, ...props }: Props) => {
  const placeholderRef = useRef<HTMLDivElement>(null);

  const itemActive = useGlobalContext(
    (c) => c.quickStartGuide.active === props.itemKey
  );
  const enabled = useGlobalContext((c) => c.quickStartGuide.enabled);
  const active = itemActive && enabled && !props.disabled;

  const { quickStartVisited } = useGlobalActions();

  function handleClick() {
    quickStartVisited(props.itemKey);
  }

  useEffect(() => {
    if (active) {
      const onClick = () => {
        handleClick();
      };
      const timeout = setTimeout(() => {
        const element = document.getElementById(elId);
        const placeholder = placeholderRef.current;
        if (placeholder && element) {
          const shape = element.getBoundingClientRect();
          placeholder.style.top = shape.top + 'px';
          placeholder.style.left = shape.left + 'px';
          placeholder.style.width = shape.width + 'px';
          placeholder.style.height = shape.height + 'px';
          placeholder.style.borderRadius = element.style.borderRadius;
          element.addEventListener('click', onClick);
        }
      });
      return () => {
        clearTimeout(timeout);
        document.getElementById(elId)?.removeEventListener('click', onClick);
      };
    }
  }, [elId, active]);

  if (!active) {
    return null;
  }

  return (
    <Portal>
      <QuickStartHighlight {...props}>
        <Box
          ref={placeholderRef}
          style={{ position: 'absolute', pointerEvents: 'none' }}
          onClick={handleClick}
        />
      </QuickStartHighlight>
    </Portal>
  );
};
