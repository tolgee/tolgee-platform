import { Delete } from '@mui/icons-material';
import { styled, IconButton, Tooltip, Checkbox } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useGlobalLoading } from 'tg.component/GlobalLoading';

import {
  useTranslationsActions,
  useTranslationsSelector,
} from './context/TranslationsContext';

const StyledContainer = styled('div')`
  position: absolute;
  display: flex;
  bottom: 0px;
  right: 0px;
  width: 100%;
  transition: all 300ms ease-in-out;
  flex-shrink: 1;
`;

const StyledContent = styled('div')`
  display: flex;
  gap: 10px;
  align-items: center;
  box-sizing: border-box;
  position: relative;
  transition: background-color 300ms ease-in-out, visibility 0ms;
  padding: ${({ theme }) => theme.spacing(0.5, 2, 0.5, 2)};
  pointer-events: all;
  border-radius: 6px;
  max-width: 100%;
  background-color: ${({ theme }) => theme.palette.emphasis[200]};
  -webkit-box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
`;

const StyledToggleAllButton = styled(IconButton)`
  display: flex;
  flex-shrink: 1;
  width: 38px;
  height: 38px;
  margin-left: 3px;
`;

export const TranslationsSelection = () => {
  const { t } = useTranslate();
  const selection = useTranslationsSelector((c) => c.selection);
  const totalCount = useTranslationsSelector((c) => c.translationsTotal || 0);
  const isLoading = useTranslationsSelector((c) => c.isLoadingAllIds);
  const isDeleting = useTranslationsSelector((c) => c.isDeleting);
  const { selectAll, selectionClear, deleteTranslations } =
    useTranslationsActions();

  const allSelected = totalCount === selection.length;
  const somethingSelected = !allSelected && Boolean(selection.length);

  const handleToggleSelectAll = () => {
    if (!allSelected) {
      selectAll();
    } else {
      selectionClear();
    }
  };

  useGlobalLoading(isLoading || isDeleting);

  return (
    <StyledContainer>
      <StyledContent>
        <Tooltip
          title={
            allSelected
              ? t('translations_clear_selection')
              : t('translations_select_all')
          }
        >
          <StyledToggleAllButton
            onClick={handleToggleSelectAll}
            data-cy="translations-select-all-button"
            size="small"
          >
            <Checkbox
              disabled={isLoading || isDeleting}
              size="small"
              checked={Boolean(selection.length)}
              indeterminate={somethingSelected}
            />
          </StyledToggleAllButton>
        </Tooltip>
        <T
          keyName="translations_selected_count"
          params={{ count: selection.length, total: totalCount }}
        />
        <Tooltip title={t('translations_delete_selected')}>
          <IconButton
            data-cy="translations-delete-button"
            onClick={deleteTranslations}
            disabled={isDeleting || !selection.length}
            size="small"
          >
            <Delete />
          </IconButton>
        </Tooltip>
      </StyledContent>
    </StyledContainer>
  );
};
