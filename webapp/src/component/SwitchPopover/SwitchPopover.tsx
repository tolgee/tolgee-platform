import React, { useEffect, useState } from 'react';
import {
  MenuItem,
  Popover,
  Autocomplete,
  InputBase,
  Box,
  IconButton,
  Tooltip,
  styled,
  Typography,
  Button,
} from '@mui/material';
import { Plus } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';
import { useDebounce } from 'use-debounce';

import { SpinnerProgress } from 'tg.component/SpinnerProgress';

const DEFAULT_SEARCH_THRESHOLD = 10;

const StyledInput = styled(InputBase)`
  padding: 5px 4px 3px 16px;
  flex-grow: 1;
`;

const StyledInputWrapper = styled(Box)`
  display: flex;
  align-items: center;
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
  padding-right: 4px;
`;

const StyledHeading = styled(Typography)`
  display: flex;
  flex-grow: 1;
  padding: 4px 4px 4px 16px;
  font-weight: 500;
`;

const StyledWrapper = styled('div')`
  display: grid;
`;

const StyledProgressContainer = styled('div')`
  display: flex;
  align-items: center;
  margin-left: -18px;
`;

function PopperComponent(props: any) {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { disablePortal, anchorEl, open, ...other } = props;
  return <Box {...other} style={{ width: '100%' }} />;
}

function PaperComponent(props: any) {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { disablePortal, anchorEl, open, ...other } = props;
  return <Box {...other} style={{ width: '100%' }} />;
}

type SwitchPopoverProps<T extends { id: number; name: string }> = {
  open: boolean;
  onClose: () => void;
  onSelect: (item: T) => void;
  anchorEl: HTMLElement;
  selectedId: number;

  // Data
  items: T[];
  isLoading: boolean;
  hasNextPage: boolean;
  fetchNextPage: () => void;
  totalElements: number;

  // Customization
  renderItem: (item: T) => React.ReactNode;
  searchPlaceholder: string;
  headingText: string;
  dataCyPrefix: string;
  searchThreshold?: number;

  // Optional "Add new" button
  onAddNew?: () => void;
  addNewTooltip?: string;

  // Search callback for a parent to handle
  onSearchChange: (search: string) => void;
};

export function SwitchPopover<T extends { id: number; name: string }>({
  open,
  onClose,
  onSelect,
  anchorEl,
  selectedId,
  items,
  isLoading,
  hasNextPage,
  fetchNextPage,
  totalElements,
  renderItem,
  searchPlaceholder,
  headingText,
  dataCyPrefix,
  searchThreshold = DEFAULT_SEARCH_THRESHOLD,
  onAddNew,
  addNewTooltip,
  onSearchChange,
}: SwitchPopoverProps<T>) {
  const [inputValue, setInputValue] = useState('');
  const { t } = useTranslate();
  const [search] = useDebounce(inputValue, 500);

  useEffect(() => {
    if (!open) {
      setInputValue('');
    }
  }, [open]);

  useEffect(() => {
    onSearchChange(search);
  }, [search, onSearchChange]);

  const [displaySearch, setDisplaySearch] = useState<boolean | undefined>(
    undefined
  );

  useEffect(() => {
    if (totalElements > 0 && displaySearch === undefined) {
      setDisplaySearch(totalElements > searchThreshold);
    }
  }, [totalElements, displaySearch, searchThreshold]);

  return (
    <Popover
      anchorEl={anchorEl}
      open={open}
      onClose={onClose}
      anchorOrigin={{
        vertical: 'top',
        horizontal: 'center',
      }}
      transformOrigin={{
        vertical: 'top',
        horizontal: 'center',
      }}
    >
      <StyledWrapper sx={{ minWidth: (anchorEl?.offsetWidth || 200) + 16 }}>
        <Autocomplete
          open
          filterOptions={(x) => x}
          loading={isLoading}
          options={items || []}
          inputValue={inputValue}
          onClose={(_, reason) => reason === 'escape' && onClose()}
          clearOnEscape={false}
          noOptionsText={t('global_nothing_found')}
          loadingText={t('global_loading_text')}
          isOptionEqualToValue={(o, v) => o.id === v.id}
          onInputChange={(_, value, reason) =>
            reason === 'input' && setInputValue(value)
          }
          getOptionLabel={({ name }) => name}
          PopperComponent={PopperComponent}
          PaperComponent={PaperComponent}
          renderOption={(props, option) => (
            <React.Fragment key={option.id}>
              <MenuItem
                {...props}
                selected={option.id === selectedId}
                data-cy={`${dataCyPrefix}-item`}
              >
                {renderItem(option)}
              </MenuItem>
              {hasNextPage && option.id === items[items.length - 1]?.id && (
                <Box display="flex" justifyContent="center" mt={0.5}>
                  <Button size="small" onClick={() => fetchNextPage()}>
                    {t('global_load_more')}
                  </Button>
                </Box>
              )}
            </React.Fragment>
          )}
          onChange={(_, newValue) => {
            if (newValue) {
              onSelect(newValue);
              onClose();
            }
          }}
          renderInput={(params) => (
            <StyledInputWrapper>
              <StyledInput
                data-cy={`${dataCyPrefix}-search`}
                key={Number(open)}
                sx={{ display: displaySearch ? undefined : 'none' }}
                ref={params.InputProps.ref}
                inputProps={params.inputProps}
                autoFocus
                placeholder={searchPlaceholder}
                endAdornment={
                  isLoading ? (
                    <StyledProgressContainer>
                      <SpinnerProgress size={18} data-cy="global-loading" />
                    </StyledProgressContainer>
                  ) : undefined
                }
              />
              {!displaySearch && <StyledHeading>{headingText}</StyledHeading>}

              {onAddNew && addNewTooltip && (
                <Tooltip title={addNewTooltip}>
                  <IconButton
                    size="small"
                    onClick={onAddNew}
                    sx={{ ml: 0.5 }}
                    data-cy={`${dataCyPrefix}-new`}
                  >
                    <Plus />
                  </IconButton>
                </Tooltip>
              )}
            </StyledInputWrapper>
          )}
        />
      </StyledWrapper>
    </Popover>
  );
}
