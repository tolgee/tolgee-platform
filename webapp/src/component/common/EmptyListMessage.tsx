import { default as React, FunctionComponent } from 'react';
import { Box, CircularProgress, Fade, styled } from '@mui/material';
import { T } from '@tolgee/react';

import { SadEmotionMessage } from './SadEmotionMessage';
import { useLoadingRegister } from 'tg.component/GlobalLoading';

const ProgressWrapper = styled('div')`
  position: absolute;
  display: flex;
  top: 0px;
  height: 400px;
  left: 0;
  right: 0;
  align-items: center;
  justify-content: center;
  pointer-events: none;
`;

type Props = {
  hint?: React.ReactNode;
  loading?: boolean;
};

export const EmptyListMessage: FunctionComponent<Props> = ({
  hint,
  loading,
  children,
}) => {
  useLoadingRegister(loading);
  return (
    <Box py={8} data-cy="global-empty-list">
      <Fade in={!loading} mountOnEnter unmountOnExit>
        <div>
          <SadEmotionMessage hint={hint}>
            {children || <T>global_empty_list_message</T>}
          </SadEmotionMessage>
        </div>
      </Fade>
      <Fade in={loading} mountOnEnter unmountOnExit>
        <ProgressWrapper>
          <CircularProgress />
        </ProgressWrapper>
      </Fade>
    </Box>
  );
};
