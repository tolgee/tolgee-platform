import React, { useEffect, useState } from 'react';
import {
  MenuItem,
  Popover,
  Autocomplete,
  InputBase,
  Box,
  styled,
  Button,
  Checkbox,
  PopoverOrigin,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useDebounce } from 'use-debounce';

import { operations } from 'tg.service/apiSchema.generated';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { User, UserAccount } from 'tg.component/UserAccount';
import { useUser } from 'tg.globalContext/helpers';

export type AssigneeFilters =
  operations['getPossibleAssignees']['parameters']['query'];

const USERS_SEARCH_TRESHOLD = 5;

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
  onSelect?: (value: User[]) => void;
  onSelectImmediate?: (value: User[]) => void;
  anchorEl: HTMLElement;
  selected: User[];
  ownedOnly?: boolean;
  projectId: number;
  anchorOrigin?: PopoverOrigin;
  transformOrigin?: PopoverOrigin;
  filters?: AssigneeFilters;
};

export const AssigneeSearchSelectPopover: React.FC<Props> = ({
  open,
  onClose,
  onSelect,
  onSelectImmediate,
  anchorEl,
  selected,
  projectId,
  anchorOrigin,
  transformOrigin,
  filters,
}) => {
  const currentUser = useUser();
  const [inputValue, setInputValue] = useState('');
  const { t } = useTranslate();
  const [search] = useDebounce(inputValue, 500);
  const [selection, setSelection] = useState(selected);

  useEffect(() => {
    setInputValue('');
    setSelection(selected);
  }, [open]);

  const query = {
    search,
    size: 20,
    sort: ['name'],
    ...filters,
  } as const satisfies AssigneeFilters;

  const usersLoadable = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/tasks/possible-assignees',
    method: 'get',
    path: { projectId },
    query: query,
    options: {
      enabled: open,
      keepPreviousData: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: { projectId },
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

  function searchMatches(items: string[]) {
    return (
      !search ||
      items.some((i) => i.toLowerCase().includes(search.toLowerCase()))
    );
  }

  const items: User[] = [
    ...(currentUser && searchMatches([currentUser.username])
      ? [currentUser]
      : []),
    ...((usersLoadable.data?.pages
      .flatMap((page) => page._embedded?.users)
      .filter(Boolean)
      .filter((u) => u?.id !== currentUser?.id) as User[]) || []),
  ];

  const [displaySearch, setDisplaySearch] = useState<boolean | undefined>(
    undefined
  );

  useEffect(() => {
    if (usersLoadable.data && displaySearch === undefined) {
      setDisplaySearch(
        usersLoadable.data.pages[0].page!.totalElements! > USERS_SEARCH_TRESHOLD
      );
    }
  }, [usersLoadable.data]);

  if (!selected) {
    return null;
  }

  return (
    <>
      <Popover
        anchorEl={anchorEl}
        open={open}
        data-cy="assignee-search-select-popover"
        onClose={() => {
          onSelect?.(selection);
          onClose();
        }}
        anchorOrigin={
          anchorOrigin ?? {
            vertical: 'top',
            horizontal: 'center',
          }
        }
        transformOrigin={
          transformOrigin ?? {
            vertical: 'top',
            horizontal: 'center',
          }
        }
      >
        <StyledWrapper sx={{ minWidth: (anchorEl?.offsetWidth || 200) + 16 }}>
          <Autocomplete
            open
            multiple
            filterOptions={(x) => x}
            loading={usersLoadable.isFetching}
            options={items || []}
            value={selection}
            inputValue={inputValue}
            onClose={(_, reason) => reason === 'escape' && onClose()}
            clearOnEscape={false}
            noOptionsText={t('global_nothing_found')}
            loadingText={t('global_loading_text')}
            isOptionEqualToValue={(o, v) => o.id === v.id}
            onInputChange={(_, value, reason) =>
              reason === 'input' && setInputValue(value)
            }
            getOptionLabel={(u) => u.name || ''}
            PopperComponent={PopperComponent}
            PaperComponent={PaperComponent}
            renderOption={(props, option) => {
              const selected = Boolean(
                selection.find((u) => u.id === option.id)
              );
              return (
                <React.Fragment key={option.id}>
                  <MenuItem
                    {...props}
                    selected={selected}
                    data-cy="user-switch-item"
                  >
                    <Checkbox
                      checked={selected}
                      size="small"
                      edge="start"
                      disableRipple
                      sx={{ marginLeft: -1, marginRight: 0.5 }}
                    />
                    <UserAccount user={option} />
                  </MenuItem>
                  {usersLoadable.hasNextPage &&
                    option.id === items![items!.length - 1].id && (
                      <Box display="flex" justifyContent="center" mt={0.5}>
                        <Button
                          size="small"
                          onClick={() => usersLoadable.fetchNextPage()}
                        >
                          {t('global_load_more')}
                        </Button>
                      </Box>
                    )}
                </React.Fragment>
              );
            }}
            onChange={(_, newValue) => {
              onSelectImmediate?.(newValue);
              setSelection(newValue);
            }}
            renderInput={(params) => (
              <StyledInputWrapper>
                <StyledInput
                  data-cy="user-switch-search"
                  key={Number(open)}
                  sx={{ display: displaySearch ? undefined : 'none' }}
                  ref={params.InputProps.ref}
                  inputProps={params.inputProps}
                  autoFocus
                  placeholder={t('global_search_user')}
                  endAdornment={
                    usersLoadable.isFetching ? (
                      <StyledProgressContainer>
                        <SpinnerProgress size={18} data-cy="global-loading" />
                      </StyledProgressContainer>
                    ) : undefined
                  }
                />
              </StyledInputWrapper>
            )}
          />
        </StyledWrapper>
      </Popover>
    </>
  );
};
