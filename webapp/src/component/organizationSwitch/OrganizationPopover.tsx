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

import { OrganizationItem } from './OrganizationItem';
import { components } from 'tg.service/apiSchema.generated';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { useConfig, useIsAdmin } from 'tg.globalContext/helpers';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';

type OrganizationModel = components['schemas']['OrganizationModel'];

const ORGANIZATION_SEARCH_TRESHOLD = 10;

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

function PopperComponent(props) {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { disablePortal, anchorEl, open, ...other } = props;
  return <Box {...other} style={{ width: '100%' }} />;
}

function PaperComponent(props) {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { disablePortal, anchorEl, open, ...other } = props;
  return <Box {...other} style={{ width: '100%' }} />;
}

type Props = {
  open: boolean;
  onClose: () => void;
  onSelect: (value: OrganizationModel) => void;
  anchorEl: HTMLElement;
  selected: OrganizationModel | undefined;
  onAddNew: () => void;
  ownedOnly?: boolean;
};

export const OrganizationPopover: React.FC<Props> = ({
  open,
  onClose,
  onSelect,
  anchorEl,
  selected,
  onAddNew,
  ownedOnly,
}) => {
  const [inputValue, setInputValue] = useState('');
  const { t } = useTranslate();
  const [search] = useDebounce(inputValue, 500);

  useEffect(() => {
    if (!open) {
      setInputValue('');
    }
  }, [open]);

  const query = {
    params: {
      filterCurrentUserOwner: Boolean(ownedOnly),
      search: search || undefined,
    },
    size: 20,
    sort: ['name'],
  };

  const organizationsLoadable = useApiInfiniteQuery({
    url: '/v2/organizations',
    method: 'get',
    query,
    options: {
      keepPreviousData: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            query: {
              ...query,
              page: lastPage.page!.number! + 1,
            },
          };
        } else {
          return null;
        }
      },
    },
  });

  const items: OrganizationModel[] = organizationsLoadable.data?.pages
    .flatMap((page) => page._embedded?.organizations)
    .filter(Boolean) as OrganizationModel[];

  const [displaySearch, setDisplaySearch] = useState<boolean | undefined>(
    undefined
  );

  const config = useConfig();
  const canCreateOrganizations =
    useIsAdmin() || config.userCanCreateOrganizations;

  useEffect(() => {
    if (organizationsLoadable.data && displaySearch === undefined) {
      setDisplaySearch(
        organizationsLoadable.data.pages[0].page!.totalElements! >
          ORGANIZATION_SEARCH_TRESHOLD
      );
    }
  }, [organizationsLoadable.data]);

  if (!selected) {
    return null;
  }

  return (
    <>
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
            loading={organizationsLoadable.isFetching}
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
                  selected={option.id === selected.id}
                  data-cy="organization-switch-item"
                >
                  <OrganizationItem data={option} />
                </MenuItem>
                {organizationsLoadable.hasNextPage &&
                  option.id === items![items!.length - 1].id && (
                    <Box display="flex" justifyContent="center" mt={0.5}>
                      <Button
                        size="small"
                        onClick={() => organizationsLoadable.fetchNextPage()}
                      >
                        {t('global_load_more')}
                      </Button>
                    </Box>
                  )}
              </React.Fragment>
            )}
            onChange={(_, newValue) => {
              onSelect?.(newValue!);
              onClose?.();
            }}
            renderInput={(params) => (
              <StyledInputWrapper>
                <StyledInput
                  data-cy="organization-switch-search"
                  key={Number(open)}
                  sx={{ display: displaySearch ? undefined : 'none' }}
                  ref={params.InputProps.ref}
                  inputProps={params.inputProps}
                  autoFocus
                  placeholder={t('global_search_organization')}
                  endAdornment={
                    organizationsLoadable.isFetching ? (
                      <StyledProgressContainer>
                        <SpinnerProgress size={18} data-cy="global-loading" />
                      </StyledProgressContainer>
                    ) : undefined
                  }
                />
                {!displaySearch && (
                  <StyledHeading>{t('organizations_title')}</StyledHeading>
                )}

                {canCreateOrganizations && (
                  <Tooltip title={t('organizations_add_new')}>
                    <IconButton
                      size="small"
                      onClick={onAddNew}
                      sx={{ ml: 0.5 }}
                      data-cy="organization-switch-new"
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
    </>
  );
};
