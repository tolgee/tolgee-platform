import { TextField, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useState } from 'react';
import { useDebouncedCallback } from 'use-debounce';

const StyledTextField = styled(TextField)`
  padding: 8px;
`;

type Props = {
  onChange: (value: string) => void;
};

export const FlagSearchField = ({ onChange }: Props) => {
  const [search, setSearch] = useState('');
  const setSearchDebounced = useDebouncedCallback(onChange, 500);
  const { t } = useTranslate();

  return (
    <StyledTextField
      placeholder={t('flag_selector_search_flag')}
      size="small"
      fullWidth
      value={search}
      onChange={(e) => {
        setSearch(e.currentTarget.value);
        setSearchDebounced(e.currentTarget.value);
      }}
    />
  );
};
