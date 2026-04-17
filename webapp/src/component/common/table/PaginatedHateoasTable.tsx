import React, { FC, JSXElementConstructor, ReactNode } from 'react';
import {
  HateoasListData,
  HateoasPaginatedData,
} from 'tg.service/response.types';
import {
  InferItemType,
  PaginatedHateoasList,
  PaginatedHateoasListProps,
} from '../list/PaginatedHateoasList';
import { Table, TableBody, TableHead } from '@mui/material';

export type PaginatedHateoasTableProps<
  WrapperComponent extends
    | keyof JSX.IntrinsicElements
    | JSXElementConstructor<any>,
  TData extends HateoasListData<TItem> | HateoasPaginatedData<TItem>,
  TItem = InferItemType<TData>
> = Omit<
  PaginatedHateoasListProps<WrapperComponent, typeof Table, TData, TItem>,
  'listComponent'
> & {
  tableHead?: ReactNode;
};

export const PaginatedHateoasTable = <
  WrapperComponent extends
    | keyof JSX.IntrinsicElements
    | JSXElementConstructor<any>,
  TData extends HateoasListData<TItem> | HateoasPaginatedData<TItem>,
  TItem = InferItemType<TData>
>(
  props: PaginatedHateoasTableProps<WrapperComponent, TData, TItem>
) => {
  const { tableHead, ...rest } = props;
  return (
    <PaginatedHateoasList
      listComponent={(listProps) => (
        <PaginatedHateoasTableListComponent
          tableHead={tableHead}
          {...listProps}
        />
      )}
      {...rest}
    />
  );
};

interface PaginatedHateoasTableListComponentProps {
  children: ReactNode;
  tableHead?: ReactNode;
}

const PaginatedHateoasTableListComponent: FC<
  PaginatedHateoasTableListComponentProps
> = ({ children, tableHead }) => {
  return (
    <Table>
      {tableHead && <TableHead>{tableHead}</TableHead>}
      <TableBody>{children}</TableBody>
    </Table>
  );
};
