import { useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Checkbox, ListItemText, Menu, MenuItem } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { SubmenuItem } from './SubmenuItem';

type LanguageModel = components['schemas']['LanguageModel'];

type Props = {
  value: number[];
  onChange: (value: number[]) => void;
  languages: LanguageModel[];
};

export const SubfilterLanguages = ({ value, onChange, languages }: Props) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);
  const handleLanguageToggle = (id: number) => () => {
    if (value.includes(id)) {
      onChange(value.filter((l) => l !== id));
    } else {
      onChange([...value, id]);
    }
  };

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('task_filter_languages')}
        onClick={() => setOpen(true)}
        selected={Boolean(value?.length)}
      />
      {open && (
        <Menu
          open={open}
          anchorEl={anchorEl.current!}
          anchorOrigin={{
            vertical: 'top',
            horizontal: 'right',
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'left',
          }}
          onClose={() => {
            setOpen(false);
          }}
          data-cy="language-select-popover"
        >
          {languages.map((lang) => (
            <MenuItem
              key={lang.tag}
              value={lang.tag}
              onClick={handleLanguageToggle(lang.id)}
            >
              <Checkbox
                checked={value?.includes(lang.id)}
                size="small"
                edge="start"
                disableRipple
              />
              <ListItemText primary={lang.name} />
            </MenuItem>
          ))}
        </Menu>
      )}
    </>
  );
};
