import { default as React, FunctionComponent, useState } from 'react';
import { IconButton, styled } from '@mui/material';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import { useCurrentLanguage, useSetLanguage } from '@tolgee/react';
import { CircledLanguageIcon } from './languages/CircledLanguageIcon';

const StyledMenu = styled(Menu)`
  .MuiPaper-root {
    margin-top: 5px;
  }
`;

const StyledIconButton = styled(IconButton)`
  width: 40px;
  height: 40px;
  img {
    user-drag: none;
  }
`;

export const LocaleMenu: FunctionComponent<{ className?: string }> = (
  props
) => {
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
      name: 'Česky',
      flag: '🇨🇿',
    },
    fr: {
      name: 'Français',
      flag: '🇫🇷',
    },
    es: {
      name: 'Español',
      flag: '🇪🇸',
    },
    de: {
      name: 'Deutsch',
      flag: '🇩🇪',
    },
  };

  const language = getCurrentLanguage();

  return (
    <>
      <div>
        <StyledIconButton
          color="inherit"
          aria-controls="language-menu"
          aria-haspopup="true"
          onClick={handleOpen}
          size="large"
        >
          <CircledLanguageIcon
            flag={languages[language]?.flag}
            size={24}
            draggable="false"
          />
        </StyledIconButton>
        <StyledMenu
          id="language-menu"
          keepMounted
          open={!!anchorEl}
          anchorEl={anchorEl}
          onClose={handleClose}
          anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'right',
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'right',
          }}
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
        </StyledMenu>
      </div>
    </>
  );
};
