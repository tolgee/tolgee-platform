import { useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Checkbox, ListItemText, Menu, MenuItem } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { SubmenuItem } from './SubmenuItem';

type SimpleProjectModel = components['schemas']['SimpleProjectModel'];
type LanguageModel = components['schemas']['LanguageModel'];

type Props = {
  value: LanguageModel[];
  onChange: (value: LanguageModel[]) => void;
  project: SimpleProjectModel;
  languages: LanguageModel[];
};

export const SubfilterLanguages = ({
  value,
  onChange,
  project,
  languages,
}: Props) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);
  const handleLanguageToggle = (language: LanguageModel) => () => {
    if (value.find((item) => item.id === language.id)) {
      onChange(value.filter((item) => item.id !== language.id));
    } else {
      onChange([...value, language]);
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
        >
          {languages.map((lang) => (
            <MenuItem
              key={lang.tag}
              value={lang.tag}
              onClick={handleLanguageToggle(lang)}
            >
              <Checkbox
                checked={Boolean(value?.find((l) => l.id === lang.id))}
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
