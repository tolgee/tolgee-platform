import React, { ComponentProps, FunctionComponent } from 'react';
import {
  Button,
  Checkbox,
  ListItemText,
  Menu,
  MenuItem,
  styled,
  Tooltip,
} from '@mui/material';
import { ArrowDropDown } from '@mui/icons-material';
import { useTranslate } from '@tolgee/react';

import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { LanguagesPermittedList } from 'tg.component/languages/LanguagesPermittedList';

const StyledButton = styled(Button)`
  padding: 0px;
  padding-left: 7px;
  padding-right: 5px;
`;

export const LanguagePermissionsMenu: FunctionComponent<{
  selected: number[];
  onSelect: (value: number[]) => void;
  buttonProps?: ComponentProps<typeof Button>;
  disabled?: boolean | number[];
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

  const disabledLanguages = Array.isArray(props.disabled)
    ? (props.disabled as number[])
    : [];

  const selectedLanguages = Array.from(
    new Set([...props.selected, ...disabledLanguages])
  )
    .map((id) => allLanguages.find((l) => l.id === id)!)
    .filter(Boolean);

  return (
    <>
      <Tooltip
        title={
          selectedLanguages?.length
            ? selectedLanguages.map((l) => l.name || l.tag).join(', ')
            : t('languages_permitted_list_all')
        }
        disableInteractive
      >
        <StyledButton
          data-cy="permissions-language-menu-button"
          disabled={props.disabled === true}
          {...props.buttonProps}
          size="small"
          variant="outlined"
          aria-controls="simple-menu"
          aria-haspopup="true"
          onClick={handleClick}
        >
          <LanguagesPermittedList
            languages={selectedLanguages}
            disabled={props.disabled}
          />
          <ArrowDropDown fontSize="small" />
        </StyledButton>
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
            disabled={
              Array.isArray(props.disabled)
                ? props.disabled.length === 0 ||
                  props.disabled.includes(lang.id)
                : props.disabled
            }
          >
            <Checkbox
              checked={
                props.selected.includes(lang.id) ||
                disabledLanguages.includes(lang.id)
              }
              size="small"
            />
            <ListItemText primary={lang.name} />
          </MenuItem>
        ))}
      </Menu>
    </>
  );
};
