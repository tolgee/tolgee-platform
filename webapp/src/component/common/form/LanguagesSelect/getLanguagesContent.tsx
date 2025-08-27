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

  // Показываем кнопки "All" и "None" только для batch операций
  const isBatchOperation = context === 'batch-operations';

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

  if (isBatchOperation) {
    return [
      // All button
      <MenuItem
        key="select-all"
        onClick={handleSelectAll}
        data-cy="translations-language-select-all"
      >
        <Checkbox checked={allSelected} size="small" />
        <ListItemText primary={t('languages_permitted_list_select_all')} />
      </MenuItem>,
      // None button
      <MenuItem
        key="select-none"
        onClick={handleSelectNone}
        data-cy="translations-language-select-none"
      >
        <Checkbox checked={false} size="small" />
        <ListItemText primary={t('llm_provider_form_select_priority_none')} />
      </MenuItem>,
      // Divider
      <Divider key="divider" />,
      // Individual language items
      ...languageItems,
    ];
  }

  // Для обычного фильтра языков возвращаем только список языков без кнопок "All" и "None"
  return languageItems;
};
