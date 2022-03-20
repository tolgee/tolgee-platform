import { FunctionComponent, useEffect, useState } from 'react';
import { IconButton, makeStyles, PaletteType } from '@material-ui/core';
import { container } from 'tsyringe';
import { ThemeService } from '../service/ThemeService';
import { Transition } from '@headlessui/react';
import { SVGProps } from 'react';
import { MoonIcon, SunIcon } from './CustomIcons';

const useStyles = makeStyles(() => ({
  sunButton: {
    color: '#EE0',
  },
  moonButton: {
    color: '#EE8',
  },
  iconButtonWrapper: {
    position: 'absolute',
  },
  wrapper: {
    position: 'relative',
    width: '40px',
    height: '40px',
  },
  inState: {
    transform: 'rotate(0deg)',
    opacity: 1,
  },
  outState: {
    transform: 'rotate(180deg)',
    opacity: 0,
  },
  svg: {
    margin: '-4px',
  },
  animation: {
    transition: 'all 0.3s ease-in-out',
  },
}));

export const ThemeMenu: FunctionComponent<{ className?: string }> = (props) => {
  const classes = useStyles();

  const [theme, setTheme] = useState<PaletteType>(
    container.resolve(ThemeService).paletteType
  );

  useEffect(() => {
    if (theme) {
      container.resolve(ThemeService).setPaletteType(theme);
    }
  }, [theme]);

  return (
    <div className={classes.wrapper}>
      <Transition
        show={theme === 'light'}
        enter={classes.animation}
        enterFrom={classes.outState}
        enterTo={classes.inState}
        leave={classes.animation}
        leaveFrom={classes.inState}
        leaveTo={classes.outState}
        className={classes.iconButtonWrapper}
      >
        <IconButton
          color="inherit"
          aria-controls="theme-menu"
          aria-haspopup="true"
          onClick={() => {
            setTheme('dark');
          }}
          className={classes.sunButton}
        >
          <SunIcon width={24} height={24} className={classes.svg} />
        </IconButton>
      </Transition>
      <Transition
        show={theme === 'dark'}
        enter={classes.animation}
        enterFrom={classes.outState}
        enterTo={classes.inState}
        leave={classes.animation}
        leaveFrom={classes.inState}
        leaveTo={classes.outState}
        className={classes.iconButtonWrapper}
      >
        <IconButton
          color="inherit"
          aria-controls="theme-menu"
          aria-haspopup="true"
          onClick={() => {
            setTheme('light');
          }}
          className={classes.moonButton}
        >
          <MoonIcon width={24} height={24} className={classes.svg} />
        </IconButton>
      </Transition>
    </div>
  );
};
