import { JSXElementConstructor, ReactNode, useEffect } from 'react';
import { Box, Grid, Typography } from '@material-ui/core';
import { Alert } from '@material-ui/lab';
import { T } from '@tolgee/react';
import { UseQueryResult } from 'react-query';

import {
  HateoasListData,
  HateoasPaginatedData,
} from 'tg.service/response.types';

import { EmptyListMessage } from '../EmptyListMessage';
import SearchField from '../form/fields/SearchField';
import { OverridableListWrappers, SimpleList } from './SimpleList';

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
    loadable: UseQueryResult<TData, any>;
    title?: ReactNode;
    sortBy?: string[];
    searchText?: string;
    onSearchChange?: (value: string) => void;
    onPageChange: (value: number) => void;
    emptyPlaceholder?: React.ReactNode;
  } & OverridableListWrappers<WrapperComponent, ListComponent>
) => {
  const { loadable } = props;

  const embedded = loadable.data?._embedded;
  const key = embedded ? Object.keys(embedded)?.[0] : '';

  const items = embedded?.[key];

  const paginationData = (loadable.data as HateoasPaginatedData<TItem>)?.page;
  const page = (paginationData?.number || 0) + 1;
  const pageCount = paginationData?.totalPages || 1;

  const handlePageChange = (page) => props.onPageChange(page - 1);

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
        <Alert color="error">
          <T>simple_paginated_list_error_message</T>
        </Alert>
      )}
      {items ? (
        <SimpleList
          pagination={{
            page,
            onPageChange: handlePageChange,
            pageCount,
          }}
          data={items}
          renderItem={props.renderItem}
          wrapperComponent={props.wrapperComponent}
          wrapperComponentProps={props.wrapperComponentProps}
          listComponent={props.listComponent}
          listComponentProps={props.listComponentProps}
        />
      ) : (
        props.emptyPlaceholder || (!loadable.isLoading && <EmptyListMessage />)
      )}
    </Box>
  );
};
