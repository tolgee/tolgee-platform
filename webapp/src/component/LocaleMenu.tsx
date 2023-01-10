import { default as React, FunctionComponent, useState } from 'react';
import { IconButton, styled } from '@mui/material';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import { CircledLanguageIcon } from './languages/CircledLanguageIcon';
import { locales } from '../locales';
import { useCurrentLanguage } from 'tg.hooks/useCurrentLanguage';
import { useTolgee } from '@tolgee/react';

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
  const tolgee = useTolgee();
  const handleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
    // @ts-ignore
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const [anchorEl, setAnchorEl] = useState(null);

  const language = useCurrentLanguage();

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
            flag={locales[language]?.flag}
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
          {Object.entries(locales).map(([abbr, lang]) => (
            <MenuItem
              selected={language === abbr}
              value={abbr}
              key={abbr}
              onClick={() => {
                handleClose();
                tolgee.changeLanguage(abbr);
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
