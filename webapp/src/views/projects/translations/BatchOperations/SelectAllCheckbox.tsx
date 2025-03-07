import { Box, Checkbox, Tooltip, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';

const StyledToggleAllButton = styled(Box)`
  width: 38px;
  height: 38px;
  margin: -1px -12px 0px -12.5px;
`;

export const SelectAllCheckbox = () => {
  const { t } = useTranslate();
  const totalCount = useTranslationsSelector((c) => c.translationsTotal || 0);
  const selection = useTranslationsSelector((c) => c.selection);
  const isLoading = useTranslationsSelector((c) => c.isLoadingAllIds);
  const allSelected = totalCount === selection.length;
  const somethingSelected = !allSelected && Boolean(selection.length);

  const { selectAll, selectionClear } = useTranslationsActions();

  const handleToggleSelectAll = () => {
    if (!allSelected) {
      selectAll();
    } else {
      selectionClear();
    }
  };

  return (
    <Tooltip
      title={
        allSelected
          ? t('translations_clear_selection')
          : t('translations_select_all')
      }
      disableInteractive
    >
      <StyledToggleAllButton>
        <Checkbox
          data-cy="translations-select-all-button"
          disabled={isLoading}
          onClick={handleToggleSelectAll}
          size="small"
          checked={Boolean(selection.length)}
          indeterminate={somethingSelected}
        />
      </StyledToggleAllButton>
    </Tooltip>
  );
};
