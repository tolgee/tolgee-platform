import { default as React, FunctionComponent } from 'react';
import { Box, Fade, CircularProgress, makeStyles } from '@material-ui/core';
import { T } from '@tolgee/react';

import { SadEmotionMessage } from './SadEmotionMessage';
import { useLoadingRegister } from 'tg.component/GlobalLoading';

const useStyles = makeStyles({
  progressWrapper: {
    position: 'absolute',
    display: 'flex',
    top: 0,
    height: 400,
    left: 0,
    right: 0,
    alignItems: 'center',
    justifyContent: 'center',
    pointerEvents: 'none',
  },
});

type Props = {
  hint?: React.ReactNode;
  loading?: boolean;
};

export const EmptyListMessage: FunctionComponent<Props> = ({
  hint,
  loading,
  children,
}) => {
  const classes = useStyles();
  useLoadingRegister(loading);
  return (
    <Box py={8} data-cy="global-empty-list" position="relative" height={500}>
      <Fade in={!loading} mountOnEnter unmountOnExit>
        <div>
          <SadEmotionMessage hint={hint}>
            {children || <T>global_empty_list_message</T>}
          </SadEmotionMessage>
        </div>
      </Fade>
      <Fade in={loading} mountOnEnter unmountOnExit>
        <div className={classes.progressWrapper}>
          <CircularProgress />
        </div>
      </Fade>
    </Box>
  );
};
