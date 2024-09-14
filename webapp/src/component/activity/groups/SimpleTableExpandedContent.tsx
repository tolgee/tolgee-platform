import React, { FC } from 'react';
import { UseQueryResult } from 'react-query';
import { PaginatedHateoasList } from '../../common/list/PaginatedHateoasList';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
} from '@mui/material';

type Loadable = UseQueryResult<{
  _embedded?: { items?: Record<string, any>[] };
}>;
type SimpleTableExpandedContentProps = {
  getData: (page: number) => Loadable;
};

export const SimpleTableExpandedContent: FC<SimpleTableExpandedContentProps> = (
  props
) => {
  const [page, setPage] = React.useState(0);

  const loadable = props.getData(page);

  const fields = getFields(loadable);

  const Table: FC = (props) => {
    return <TheTable headItems={fields}>{props.children}</TheTable>;
  };

  return (
    <PaginatedHateoasList
      listComponent={Table}
      renderItem={(i) => (
        <TableRow>
          {fields.map((f, idx) => (
            <TableCell key={`${idx}+${f}`}>{i[f]}</TableCell>
          ))}
        </TableRow>
      )}
      onPageChange={(p) => setPage(p)}
      loadable={loadable}
    />
  );
};

const TheTable: FC<{ headItems: string[] }> = (props) => {
  return (
    <Table>
      <TableHead>
        {props.headItems.map((item, idx) => (
          <TableCell key={`${idx}+${item}`}>{item}</TableCell>
        ))}
      </TableHead>
      <TableBody>{props.children}</TableBody>
    </Table>
  );
};

function getFields(loadable: Loadable) {
  const fields = new Set<string>();
  loadable.data?._embedded?.items?.forEach((i) => {
    Object.keys(i).forEach((k) => fields.add(k));
  });

  return [...fields];
}
