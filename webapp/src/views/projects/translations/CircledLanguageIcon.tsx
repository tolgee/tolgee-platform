import React from 'react';
import { Box, makeStyles } from '@material-ui/core';
import { FlagImage } from 'tg.component/languages/FlagImage';

type Props = {
  flag: string | null | undefined;
};

const useStyles = makeStyles((theme) => ({
  icon: {
    borderRadius: '50%',
    overflow: 'hidden',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    flexShrink: 0,
    position: 'relative',
  },
  shadow: {
    top: 0,
    borderRadius: '50%',
    boxShadow: 'inset 0px 0px 0px 1px rgba(0,0,0,0.2)',
    content: '',
    display: 'block',
    height: '100%',
    position: 'absolute',
    width: '100%',
  },
}));

export const CircledLanguageIcon: React.FC<Props> = ({ flag }) => {
  const classes = useStyles();
  const size = 18;
  return (
    <Box className={classes.icon} width={size} height={size}>
      <FlagImage
        height={Math.floor(size * 0.7) * 2}
        width={Math.floor(size * 0.7) * 2}
        flagEmoji={flag || ''}
      />
      <div className={classes.shadow} />
    </Box>
  );
};
