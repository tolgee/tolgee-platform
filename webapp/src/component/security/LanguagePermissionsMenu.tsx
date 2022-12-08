import React, { ComponentProps, FunctionComponent } from 'react';
import {
  Menu,
  MenuItem,
  Checkbox,
  ListItemText,
  Button,
  Tooltip,
} from '@mui/material';
import { ArrowDropDown } from '@mui/icons-material';

import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { LanguagesPermittedList } from 'tg.component/languages/LanguagesPermittedList';
import { useTranslate } from '@tolgee/react';

export const LanguagePermissionsMenu: FunctionComponent<{
  selected: number[];
  onSelect: (value: number[]) => void;
  buttonProps?: ComponentProps<typeof Button>;
}> = (props) => {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const { t } = useTranslate();

  const allLanguages = useProjectLanguages();

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleToggle = (langId: number) => () => {
    if (props.selected.includes(langId)) {
      props.onSelect(props.selected.filter((id) => id !== langId));
    } else {
      props.onSelect([...props.selected, langId]);
    }
  };

  const selectedLanguages = props.selected
    .map((id) => allLanguages.find((l) => l.id === id)!)
    .filter(Boolean);

  return (
    <>
      <Tooltip
        title={t('permission_languages_hint', {
          subject: selectedLanguages?.length
            ? selectedLanguages.map((l) => l.name || l.tag).join(', ')
            : t('languages_permitted_list_all'),
        })}
      >
        <Button
          data-cy="permissions-language-menu-button"
          {...props.buttonProps}
          size="small"
          variant="outlined"
          aria-controls="simple-menu"
          aria-haspopup="true"
          onClick={handleClick}
        >
          <LanguagesPermittedList languages={selectedLanguages} />
          <ArrowDropDown fontSize="small" />
        </Button>
      </Tooltip>
      <Menu
        data-cy="permissions-languages-menu"
        id="simple-menu"
        anchorEl={anchorEl}
        keepMounted
        open={Boolean(anchorEl)}
        onClose={handleClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'left',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'left',
        }}
      >
        {allLanguages.map((lang) => (
          <MenuItem
            key={lang.id}
            value={lang.tag}
            data-cy="translations-language-select-item"
            onClick={handleToggle(lang.id)}
          >
            <Checkbox checked={props.selected.includes(lang.id)} size="small" />
            <ListItemText primary={lang.name} />
          </MenuItem>
        ))}
      </Menu>
    </>
  );
};
