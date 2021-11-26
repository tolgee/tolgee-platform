import { default as React, FunctionComponent, useState } from 'react';
import { IconButton, makeStyles } from '@material-ui/core';
import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import { useCurrentLanguage, useSetLanguage } from '@tolgee/react';
import { CircledLanguageIcon } from './languages/CircledLanguageIcon';

const useStyles = makeStyles((theme) => ({
  paper: {
    border: '1px solid #d3d4d5',
    marginTop: 5,
  },
  iconButton: {
    width: 40,
    height: 40,
  },
  circledIcon: {
    '& > img': {
      userDrag: 'none',
    },
  },
}));

export const LocaleMenu: FunctionComponent<{ className?: string }> = (
  props
) => {
  const classes = useStyles();
  const handleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
    // @ts-ignore
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const [anchorEl, setAnchorEl] = useState(null);

  const setLanguage = useSetLanguage();
  const getCurrentLanguage = useCurrentLanguage();

  const languages = {
    en: {
      name: 'English',
      flag: '🇬🇧',
    },
    cs: {
      name: 'česky',
      flag: '🇨🇿',
    },
    fr: {
      name: 'Français',
      flag: '🇫🇷',
    },
    es: {
      name: 'español',
      flag: '🇪🇸',
    },
  };

  const language = getCurrentLanguage();

  return (
    <>
      <div>
        <IconButton
          color="inherit"
          aria-controls="language-menu"
          aria-haspopup="true"
          onClick={handleOpen}
          className={classes.iconButton}
        >
          <CircledLanguageIcon
            flag={languages[language]?.flag}
            size={24}
            draggable="false"
            className={classes.circledIcon}
          />
        </IconButton>
        <Menu
          id="language-menu"
          keepMounted
          open={!!anchorEl}
          anchorEl={anchorEl}
          onClose={handleClose}
          elevation={0}
          getContentAnchorEl={null}
          anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'right',
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'right',
          }}
          classes={{ paper: classes.paper }}
        >
          {Object.entries(languages).map(([abbr, lang]) => (
            <MenuItem
              selected={getCurrentLanguage() === abbr}
              value={abbr}
              key={abbr}
              onClick={() => {
                handleClose();
                setLanguage(abbr);
              }}
            >
              {lang.name}
            </MenuItem>
          ))}
        </Menu>
      </div>
    </>
  );
};
