import { JSXElementConstructor, ReactNode, useEffect, useState } from 'react';
import { Box, Grid, Typography } from '@mui/material';
import { Alert } from '@mui/material';
import { T } from '@tolgee/react';

import {
  AbstractLoadableActions,
  StateWithLoadables,
} from 'tg.store/AbstractLoadableActions';

import { BoxLoading } from '../BoxLoading';
import { EmptyListMessage } from '../EmptyListMessage';
import SearchField from '../form/fields/SearchField';
import { OverridableListWrappers, SimpleList } from './SimpleList';
import {
  EmbeddedDataItem,
  usePaginatedHateoasDataHelper,
} from './usePaginatedHateoasDataHelper';
import { useGlobalLoading } from 'tg.component/GlobalLoading';

export type SimplePaginatedHateoasListProps<
  ActionsType extends AbstractLoadableActions<StateWithLoadables<ActionsType>>,
  LoadableName extends keyof ActionsType['loadableDefinitions'],
  WrapperComponent extends
    | keyof JSX.IntrinsicElements
    | JSXElementConstructor<any>,
  ListComponent extends keyof JSX.IntrinsicElements | JSXElementConstructor<any>
> = {
  renderItem: (
    itemData: EmbeddedDataItem<ActionsType, LoadableName>
  ) => ReactNode;
  actions: ActionsType;
  loadableName: LoadableName;
  dispatchParams?: [
    Omit<
      Parameters<
        ActionsType['loadableDefinitions'][LoadableName]['payloadProvider']
      >[0],
      'query'
    > & {
      query?: Omit<
        Parameters<
          ActionsType['loadableDefinitions'][LoadableName]['payloadProvider']
        >[0]['query'],
        'pageable'
      >;
    },
    ...any[]
  ];
  pageSize?: number;
  title?: ReactNode;
  searchField?: boolean;
  sortBy?: string[];
  searchText?: string;
} & OverridableListWrappers<WrapperComponent, ListComponent>;

export function SimplePaginatedHateoasList<
  ActionsType extends AbstractLoadableActions<any>,
  LoadableName extends keyof ActionsType['loadableDefinitions'],
  WrapperComponent extends
    | keyof JSX.IntrinsicElements
    | JSXElementConstructor<any>,
  ListComponent extends keyof JSX.IntrinsicElements | JSXElementConstructor<any>
>(
  props: SimplePaginatedHateoasListProps<
    ActionsType,
    LoadableName,
    WrapperComponent,
    ListComponent
  >
) {
  const [search, setSearch] = useState(undefined as string | undefined);

  const helper = usePaginatedHateoasDataHelper({
    ...props,
    search: search || props.searchText,
  });

  useGlobalLoading(helper.loading && helper.items);

  useEffect(() => {
    setSearch(props.searchText);
  }, [props.searchText]);

  if (helper.loading && !helper.items) {
    return <BoxLoading />;
  }

  return (
    <Box data-cy="global-paginated-list">
      {' '}
      {(props.title || props.searchField) && (
        <Box mb={1}>
          <Grid container alignItems="center">
            <Grid item lg md sm xs>
              {props.title && (
                <Box>
                  <Typography variant="h6">{props.title}</Typography>
                </Box>
              )}
            </Grid>
            {props.searchField && (
              <Grid item lg={4} md={5} sm={12} xs={12}>
                <SearchField
                  data-cy="global-list-search"
                  fullWidth
                  initial={search || ''}
                  onSearch={(val) => {
                    setSearch(val);
                  }}
                  variant={'outlined'}
                  size="small"
                />
              </Grid>
            )}
          </Grid>
        </Box>
      )}
      {helper.error && (
        <Alert severity="error">
          <T>simple_paginated_list_error_message</T>
        </Alert>
      )}
      {!helper.error &&
        (!helper.items ? (
          <EmptyListMessage />
        ) : (
          <SimpleList
            pagination={{
              page: (helper.page?.number as number) + 1,
              onPageChange: helper.onPageChange,
              pageCount: helper.page.totalPages || 0,
            }}
            data={helper.items}
            renderItem={props.renderItem}
            wrapperComponent={props.wrapperComponent}
            wrapperComponentProps={props.wrapperComponentProps}
            listComponent={props.listComponent}
            listComponentProps={props.listComponentProps}
          />
        ))}
    </Box>
  );
}
