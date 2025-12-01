import { FC, ReactNode, useMemo, useState } from 'react';
import {
  Box,
  IconButton,
  InputAdornment,
  TextField,
  styled,
} from '@mui/material';
import { Plus, XClose, SearchSm } from '@untitled-ui/icons-react';
import { Autocomplete } from '@mui/material';
import { suggest } from '@tginternal/language-util';
import { SuggestResult } from '@tginternal/language-util/lib/suggesting';
import { T, useTranslate } from '@tolgee/react';
import { MenuItem } from '@mui/material';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';

const StyledMenuItem = styled(MenuItem)`
  display: flex;
  gap: 12px;
`;

export type AutocompleteOption = Omit<SuggestResult, 'languageId'> & {
  isNew?: true;
  languageId: string;
  customRenderOption?: ReactNode;
};

const getOptions = (input: string): AutocompleteOption[] => {
  if (!input) {
    return suggest(input);
  }

  const newLang: AutocompleteOption = {
    isNew: true,
    flags: ['🏳️'],
    languageId: 'custom',
    originalName: '',
    englishName: input,
    customRenderOption: (
      <Box
        display="inline-flex"
        justifyContent="center"
        gap={1}
        alignItems="center"
      >
        <Plus fontSize="small" />
        <T keyName="language_field_autocomplete_label_new_language" />
      </Box>
    ),
  };
  return [newLang, ...suggest(input)];
};

export const LanguageAutocomplete: FC<{
  onSelect: (value: AutocompleteOption) => void;
  onClear?: () => void;
  autoFocus?: boolean;
  existingLanguages: string[];
}> = (props) => {
  const [options, setOptions] = useState([] as AutocompleteOption[]);
  const [search, setSearch] = useState('');
  const existingLanguages = useMemo(
    () => new Set(props.existingLanguages),
    [props.existingLanguages]
  );
  const { t } = useTranslate();

  return (
    <Autocomplete
      id="language-name-select"
      options={options}
      onOpen={() => setOptions(getOptions(''))}
      filterOptions={(options) => options}
      getOptionLabel={(option) => option.englishName}
      inputValue={search}
      onInputChange={(_, value) => setSearch(value)}
      value={null}
      onChange={(_, value) => {
        props.onSelect(value as AutocompleteOption);
        setSearch('');
      }}
      renderOption={(props, option) => {
        const itemContent = option.customRenderOption || (
          <>
            <FlagImage height={16} flagEmoji={option.flags?.[0] || ''} />
            <span>{`${option.englishName} - ${option.originalName} - ${option.languageId} `}</span>
          </>
        );
        return existingLanguages.has(option.languageId) ? (
          <StyledMenuItem
            key={option.languageId + ':disabled'}
            disabled={true}
            data-cy="languages-create-autocomplete-suggested-option"
          >
            {itemContent}
          </StyledMenuItem>
        ) : (
          <StyledMenuItem
            {...props}
            key={option.languageId}
            data-cy="languages-create-autocomplete-suggested-option"
          >
            {itemContent}
          </StyledMenuItem>
        );
      }}
      renderInput={(params) => (
        <TextField
          data-cy="languages-create-autocomplete-field"
          {...params}
          onChange={(e) => {
            setTimeout(() => setOptions(getOptions(e.target.value)));
          }}
          placeholder={t('language_create_autocomplete_label')}
          margin="normal"
          variant="outlined"
          size="small"
          InputProps={{
            autoFocus: props.autoFocus,
            ...params.InputProps,
            style: { paddingRight: 0 },
            startAdornment: (
              <InputAdornment position="end">
                <SearchSm width={20} height={20} />
              </InputAdornment>
            ),
            endAdornment: props.onClear ? (
              <InputAdornment position="end">
                <IconButton size="small" onClick={props.onClear}>
                  <XClose />
                </IconButton>
              </InputAdornment>
            ) : undefined,
          }}
        />
      )}
    />
  );
};
