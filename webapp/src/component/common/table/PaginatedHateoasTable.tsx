import React, { FC, JSXElementConstructor } from 'react';
import {
  HateoasListData,
  HateoasPaginatedData,
} from 'tg.service/response.types';
import {
  InferItemType,
  PaginatedHateoasList,
  PaginatedHateoasListProps,
} from '../list/PaginatedHateoasList';
import { Table, TableBody } from '@mui/material';

export type PaginatedHateoasTableProps<
  WrapperComponent extends
    | keyof JSX.IntrinsicElements
    | JSXElementConstructor<any>,
  TData extends HateoasListData<TItem> | HateoasPaginatedData<TItem>,
  TItem = InferItemType<TData>
> = Omit<
  PaginatedHateoasListProps<WrapperComponent, typeof Table, TData, TItem>,
  'listComponent'
>;

export const PaginatedHateoasTable = <
  WrapperComponent extends
    | keyof JSX.IntrinsicElements
    | JSXElementConstructor<any>,
  TData extends HateoasListData<TItem> | HateoasPaginatedData<TItem>,
  TItem = InferItemType<TData>
>(
  props: PaginatedHateoasTableProps<WrapperComponent, TData, TItem>
) => {
  return (
    <PaginatedHateoasList
      listComponent={PaginatedHateoasTableListComponent}
      {...props}
    />
  );
};

const PaginatedHateoasTableListComponent: FC = ({ children }) => {
  return (
    <Table>
      <TableBody>{children}</TableBody>
    </Table>
  );
};
