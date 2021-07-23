import React from 'react';
import { Box, makeStyles } from '@material-ui/core';
import { FlagImage } from 'tg.component/languages/FlagImage';

type Props = {
  flag: string | null | undefined;
  size?: number;
};

const useStyles = makeStyles((theme) => ({
  wrapper: {
    padding: 2,
  },
  iconWrapper: {
    overflow: 'hidden',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    flexShrink: 0,
    position: 'relative',
    borderRadius: '50%',
    filter: 'drop-shadow(0px 0px 1px rgba(0, 0, 0, 0.2))',
  },
  iconImage: {
    position: 'absolute',
  },
  iconImageBlur: {
    zIndex: 2,
    position: 'absolute',
    filter: 'blur(1px)',
    opacity: '0.05',
  },
}));

export const CircledLanguageIcon: React.FC<Props> = ({ flag, size }) => {
  const classes = useStyles();
  size = size || 18;
  return (
    <Box className={classes.wrapper}>
      <Box className={classes.iconWrapper} width={size} height={size}>
        <FlagImage
          className={classes.iconImage}
          height={Math.floor(size * 0.75) * 2}
          width={Math.floor(size * 0.75) * 2}
          flagEmoji={flag || ''}
        />
        <FlagImage
          className={classes.iconImageBlur}
          height={Math.floor(size * 0.75) * 2}
          width={Math.floor(size * 0.75) * 2}
          flagEmoji={flag || ''}
        />
      </Box>
    </Box>
  );
};
