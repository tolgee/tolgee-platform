import React from 'react';
import { IconButton, styled, Tooltip } from '@mui/material';
import { stopBubble } from 'tg.fixtures/eventHandler';

const StyledIconButton = styled(IconButton)`
  display: flex;
  cursor: pointer;
  width: 36px;
  height: 36px;
  margin: -8px;

  & svg {
    width: 20px;
    height: 20px;
  }
`;

type Props = React.ComponentProps<typeof IconButton> & {
  tooltip?: React.ReactNode;
};

export const ControlsButton: React.FC<Props> = React.forwardRef(
  function ControlsButton(
    { children, className, onClick, tooltip, ...props },
    ref
  ) {
    const content = (
      <StyledIconButton
        size="small"
        className={className}
        onClick={stopBubble(onClick)}
        ref={ref}
        {...props}
      >
        {children}
      </StyledIconButton>
    );

    return tooltip ? (
      <Tooltip disableInteractive title={tooltip}>
        {content}
      </Tooltip>
    ) : (
      content
    );
  }
);
