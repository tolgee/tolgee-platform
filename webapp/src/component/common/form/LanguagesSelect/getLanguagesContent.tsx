import { container } from 'tsyringe';
import { Checkbox, ListItemText, MenuItem } from '@mui/material';
import { T } from '@tolgee/react';

import { putBaseLangFirst } from 'tg.fixtures/putBaseLangFirst';
import { components } from 'tg.service/apiSchema.generated';
import { MessageService } from 'tg.service/MessageService';

type LanguageModel = components['schemas']['LanguageModel'];

type Props = {
  onChange: (value: string[]) => void;
  languages: LanguageModel[];
  value: string[];
};

const messaging = container.resolve(MessageService);

export const getLanguagesContent = ({ languages, value, onChange }: Props) => {
  const handleLanguageChange = (lang: string) => () => {
    const baseLang = languages.find((l) => l.base)?.tag;
    const result = value.includes(lang)
      ? value.filter((l) => l !== lang)
      : putBaseLangFirst([...value, lang], baseLang);

    if (!result?.length) {
      messaging.error(<T>set_at_least_one_language_error</T>);
      return;
    }
    onChange(result);
  };

  return languages.map((lang) => (
    <MenuItem
      key={lang.tag}
      value={lang.tag}
      data-cy="translations-language-select-item"
      onClick={handleLanguageChange(lang.tag)}
    >
      <Checkbox checked={value?.includes(lang.tag)} size="small" />
      <ListItemText primary={lang.name} />
    </MenuItem>
  ));
};
