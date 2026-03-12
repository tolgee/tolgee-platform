import { Checkbox, ListItemText, MenuItem, Divider } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { putBaseLangFirst } from 'tg.fixtures/putBaseLangFirst';
import { components } from 'tg.service/apiSchema.generated';
import { messageService } from 'tg.service/MessageService';

type LanguageModel = components['schemas']['LanguageModel'];

type Props = {
  onChange: (value: string[]) => void;
  languages: LanguageModel[];
  value: string[];
  disabledLanguages: number[] | undefined;
  enableEmpty?: boolean;
  context?: string;
};

export const getLanguagesContent = ({
  languages,
  value,
  onChange,
  disabledLanguages,
  enableEmpty,
  context,
}: Props) => {
  const { t } = useTranslate();

  const handleLanguageChange = (lang: string) => () => {
    const baseLang = languages.find((l) => l.base)?.tag;
    const result = value.includes(lang)
      ? value.filter((l) => l !== lang)
      : putBaseLangFirst([...value, lang], baseLang);

    if (!result?.length && !enableEmpty) {
      messageService.error(<T keyName="set_at_least_one_language_error" />);
      return;
    }
    onChange(result || []);
  };

  const handleSelectAll = () => {
    const availableLanguages = languages
      .filter((lang) => !disabledLanguages?.includes(lang.id))
      .map((lang) => lang.tag);
    const baseLang = languages.find((l) => l.base)?.tag;
    const result = putBaseLangFirst(availableLanguages, baseLang);
    onChange(result || []);
  };

  const handleSelectNone = () => {
    if (!enableEmpty) {
      messageService.error(<T keyName="set_at_least_one_language_error" />);
      return;
    }
    onChange([]);
  };

  const availableLanguages = languages.filter(
    (lang) => !disabledLanguages?.includes(lang.id)
  );
  const allSelected = availableLanguages.every((lang) =>
    value.includes(lang.tag)
  );

  const isBatchOperation = context === 'batch-operations';
  const isTranslations = context === 'translations';

  const baseLang = languages.find((l) => l.base)?.tag;
  const isOnlyBaseSelected = value.length === 1 && value[0] === baseLang;

  const handleClear = () => {
    if (baseLang) {
      onChange([baseLang]);
    }
  };

  const languageItems = languages.map((lang) => (
    <MenuItem
      key={lang.tag}
      value={lang.tag}
      data-cy="translations-language-select-item"
      onClick={handleLanguageChange(lang.tag)}
      disabled={disabledLanguages?.includes(lang.id)}
    >
      <Checkbox checked={value?.includes(lang.tag)} size="small" />
      <ListItemText primary={lang.name} />
    </MenuItem>
  ));

  if (isTranslations) {
    return [
      <MenuItem
        key="clear"
        onClick={handleClear}
        disabled={isOnlyBaseSelected}
        data-cy="translations-language-select-clear"
      >
        <ListItemText primary={t('languages_select_clear')} />
      </MenuItem>,
      <Divider key="divider" />,
      ...languageItems,
    ];
  }

  if (isBatchOperation) {
    return [
      <MenuItem
        key="select-all"
        onClick={handleSelectAll}
        data-cy="translations-language-select-all"
      >
        <Checkbox checked={allSelected} size="small" />
        <ListItemText primary={t('languages_permitted_list_select_all')} />
      </MenuItem>,
      <MenuItem
        key="select-none"
        onClick={handleSelectNone}
        data-cy="translations-language-select-none"
      >
        <Checkbox checked={false} size="small" />
        <ListItemText primary={t('llm_provider_form_select_priority_none')} />
      </MenuItem>,
      <Divider key="divider" />,
      ...languageItems,
    ];
  }

  return languageItems;
};
