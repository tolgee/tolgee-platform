import { JSXElementConstructor, ReactNode, useEffect } from 'react';
import { Alert, Box, Grid, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { UseQueryResult } from 'react-query';

import {
  HateoasListData,
  HateoasPaginatedData,
} from 'tg.service/response.types';

import { EmptyListMessage } from '../EmptyListMessage';
import SearchField from '../form/fields/SearchField';
import { OverridableListWrappers, SimpleList } from './SimpleList';
import { TranslatedError } from 'tg.translationTools/TranslatedError';

// get item type from TData
export type InferItemType<TData> = TData extends HateoasListData<
  infer ItemDataType
>
  ? ItemDataType
  : never;

export const PaginatedHateoasList = <
  WrapperComponent extends
    | keyof JSX.IntrinsicElements
    | JSXElementConstructor<any>,
  ListComponent extends
    | keyof JSX.IntrinsicElements
    | JSXElementConstructor<any>,
  TData extends HateoasListData<TItem> | HateoasPaginatedData<TItem>,
  TItem = InferItemType<TData>
>(
  props: {
    renderItem: (itemData: TItem) => ReactNode;
    itemSeparator?: () => ReactNode;
    loadable: UseQueryResult<TData, any>;
    title?: ReactNode;
    sortBy?: string[];
    searchText?: string;
    onSearchChange?: (value: string) => void;
    onPageChange?: (value: number) => void;
    emptyPlaceholder?: React.ReactNode;
    getKey?: (value: TItem) => any;
  } & OverridableListWrappers<WrapperComponent, ListComponent>
) => {
  const { loadable } = props;

  const embedded = loadable.data?._embedded;
  const key = embedded ? Object.keys(embedded)?.[0] : '';

  const items = embedded?.[key];

  const paginationData = (loadable.data as HateoasPaginatedData<TItem>)?.page;
  const page = (paginationData?.number || 0) + 1;
  const pageCount = paginationData?.totalPages || 1;

  const handlePageChange = (page) => props.onPageChange?.(page - 1);

  useEffect(() => {
    if (page > pageCount) {
      handlePageChange(pageCount);
    }
  });

  return (
    <Box data-cy="global-paginated-list">
      {(props.title || props.onSearchChange) && (
        <Box mb={1}>
          <Grid container alignItems="center">
            <Grid item lg md sm xs>
              {props.title && (
                <Box>
                  <Typography variant="h6">{props.title}</Typography>
                </Box>
              )}
            </Grid>
            {props.onSearchChange && (
              <Grid item lg={4} md={5} sm={12} xs={12}>
                <SearchField
                  data-cy="global-list-search"
                  fullWidth
                  initial={props.searchText}
                  onSearch={props.onSearchChange}
                  variant={'outlined'}
                  size="small"
                />
              </Grid>
            )}
          </Grid>
        </Box>
      )}
      {loadable.error && (
        <Box mt={2}>
          <Alert severity="error">
            {typeof loadable.error.code === 'string' ? (
              <TranslatedError code={loadable.error.code} />
            ) : (
              <T keyName="simple_paginated_list_error_message" />
            )}
          </Alert>
        </Box>
      )}
      {items ? (
        <SimpleList
          pagination={
            props.onPageChange
              ? {
                  page,
                  onPageChange: handlePageChange,
                  pageCount,
                }
              : undefined
          }
          data={items}
          renderItem={props.renderItem}
          itemSeparator={props.itemSeparator}
          wrapperComponent={props.wrapperComponent}
          wrapperComponentProps={props.wrapperComponentProps}
          listComponent={props.listComponent}
          listComponentProps={props.listComponentProps}
          getKey={props.getKey}
        />
      ) : (
        !loadable.isLoading &&
        !loadable.isError &&
        (props.emptyPlaceholder || <EmptyListMessage />)
      )}
    </Box>
  );
};
