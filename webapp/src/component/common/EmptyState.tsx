import React, { ComponentProps } from 'react';
import { Box, Fade, styled } from '@mui/material';

import { SadEmotionMessageProps } from './SadEmotionMessage';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';

const ProgressWrapper = styled('div')`
  display: flex;
  top: 0px;
  height: ${(props: any) => props.height || '400px'};
  left: 0;
  right: 0;
  align-items: center;
  justify-content: center;
  pointer-events: none;
`;

type Props = {
  loading?: boolean;
  wrapperProps?: ComponentProps<typeof Box>;
  children: React.ReactNode;
} & SadEmotionMessageProps;

export const EmptyState: React.FC<Props> = ({
  loading,
  children,
  wrapperProps,
}) => {
  wrapperProps = {
    ...wrapperProps,
    py: wrapperProps?.py || 8,
  } as any;

  return (
    <Box {...wrapperProps}>
      <Fade in={!loading} mountOnEnter unmountOnExit>
        <div data-cy="global-empty-state">{children}</div>
      </Fade>
      <Fade in={loading} mountOnEnter unmountOnExit>
        <ProgressWrapper>
          <SpinnerProgress />
        </ProgressWrapper>
      </Fade>
    </Box>
  );
};
