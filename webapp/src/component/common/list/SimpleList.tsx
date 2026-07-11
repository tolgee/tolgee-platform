import React, { JSXElementConstructor, ReactNode } from 'react';
import { Box, List, Paper, PaperProps } from '@mui/material';
import { Pagination } from '@mui/material';

export type OverridableListWrappers<
  WrapperComponent extends
    | keyof JSX.IntrinsicElements
    | JSXElementConstructor<any> = typeof Paper,
  ListComponent extends
    | keyof JSX.IntrinsicElements
    | JSXElementConstructor<any> = typeof List
> = {
  wrapperComponent?: WrapperComponent;
  wrapperComponentProps?: React.ComponentProps<WrapperComponent>;
  listComponent?: ListComponent;
  listComponentProps?: React.ComponentProps<ListComponent>;
};

export const SimpleList = <
  DataItem,
  WrapperComponent extends
    | keyof JSX.IntrinsicElements
    | JSXElementConstructor<any>,
  ListComponent extends keyof JSX.IntrinsicElements | JSXElementConstructor<any>
>(
  props: {
    data: DataItem[];
    pagination?: {
      page: number;
      pageCount: number;
      onPageChange: (page: number) => void;
    };
    renderItem: (item: DataItem) => ReactNode;
    itemSeparator?: () => ReactNode;
    getKey?: (item: DataItem) => React.Key;
  } & OverridableListWrappers<WrapperComponent, ListComponent>
) => {
  const { data, pagination } = props;

  const Wrapper = props.wrapperComponent || Paper;
  const baseProps =
    Wrapper === Paper ? ({ variant: 'outlined' } as PaperProps) : {};
  const wrapperProps = { ...baseProps, ...props.wrapperComponentProps };
  const ListWrapper = props.listComponent || List;

  return (
    <>
      <Wrapper {...wrapperProps}>
        <ListWrapper data-cy="global-list-items" {...props.listComponentProps}>
          {data.map((item, index) => (
            <React.Fragment
              key={props.getKey?.(item) ?? (item as any).id ?? index}
            >
              {props.renderItem(item)}
              {index < data.length - 1 && props.itemSeparator?.()}
            </React.Fragment>
          ))}
        </ListWrapper>
        {pagination && pagination.pageCount > 1 && (
          <Box display="flex" justifyContent="flex-end" mt={1} mb={1}>
            <Pagination
              data-cy="global-list-pagination"
              page={pagination.page}
              count={pagination.pageCount}
              onChange={(_, value) => props.pagination?.onPageChange(value)}
            />
          </Box>
        )}
      </Wrapper>
    </>
  );
};
