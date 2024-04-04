import { FC, useState } from 'react';
import { Box } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';

import {
  AutocompleteOption,
  LanguageAutocomplete,
} from './LanguageAutocomplete';
import { LanguageModifyForm } from './LanguageModifyForm';
import { PreparedLanguage } from './PreparedLanguage';

type LanguageRequest = components['schemas']['LanguageRequest'];

type Props = {
  value: LanguageRequest[];
  onChange: (value: LanguageRequest[]) => void;
  autoFocus?: boolean;
  existingTags: string[];
};

export const CreateLanguagesField: FC<Props> = ({
  value,
  onChange,
  autoFocus,
  existingTags,
}) => {
  const [preferredEmojis, setPreferredEmojis] = useState([] as string[]);
  const [editIndex, setEditIndex] = useState<number>();
  const [editValue, setEditValue] = useState<LanguageRequest>();

  const existingLangs = [...existingTags, ...value.map((l) => l.tag)];

  const onSelectInAutocomplete = (option: AutocompleteOption) => {
    if (option) {
      setPreferredEmojis(option.flags);
      const item: LanguageRequest = {
        name: option.englishName,
        originalName: option.originalName,
        tag: option.languageId,
        flagEmoji: option.flags?.[0] || '',
      };
      if (option.isNew) {
        setEditValue(item);
      } else {
        onChange([...value, item]);
      }
    }
  };

  const handleRemoveFromList = (index: number) => {
    onChange(value.filter((_, i) => i !== index));
  };

  return (
    <>
      <Box display="grid">
        {value.length > 0 && (
          <Box display="flex" gap={1} flexWrap="wrap">
            {value.map((item, i) => (
              <PreparedLanguage
                {...item}
                key={i}
                onReset={() => {
                  handleRemoveFromList(i);
                }}
                onEdit={() => {
                  setEditIndex(i);
                  setEditValue(item);
                }}
              />
            ))}
          </Box>
        )}
        <LanguageAutocomplete
          autoFocus={autoFocus}
          onSelect={onSelectInAutocomplete}
          existingLanguages={existingLangs}
        />
      </Box>
      {editValue && (
        <LanguageModifyForm
          inDialog={true}
          onModified={(newValue) => {
            if (editIndex !== undefined) {
              onChange(
                value.map((item, i) => {
                  if (i === editIndex) {
                    return newValue;
                  } else {
                    return item;
                  }
                })
              );
            } else {
              onChange([...value, newValue]);
            }
            setEditIndex(undefined);
            setEditValue(undefined);
          }}
          onCancel={() => {
            setEditIndex(undefined);
            setEditValue(undefined);
          }}
          values={editValue}
          preferredEmojis={preferredEmojis}
          existingTags={existingLangs.filter((tag) => editValue.tag !== tag)}
        />
      )}
    </>
  );
};
