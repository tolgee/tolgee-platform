import React, { FC, useState } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
} from '@mui/material';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { components } from 'tg.service/apiSchema.generated';

type GenericGroupItemModel = components['schemas']['GenericGroupItemModel'];

const DISPLAYED_DESCRIPTION_FIELDS = ['name', 'text', 'key', 'tag', 'username'];

export const GenericGroupExpandedContent: FC<{ groupId: number }> = (props) => {
  const project = useProject();
  const [page, setPage] = useState(0);

  const loadable = useApiQuery({
    url: '/v2/projects/{projectId}/activity/group-items/generic/{groupId}',
    method: 'get',
    path: { projectId: project.id, groupId: props.groupId },
    query: { page, size: 20 },
  });

  return (
    <PaginatedHateoasList
      loadable={loadable}
      onPageChange={(p) => setPage(p)}
      listComponent={ItemsTable}
      renderItem={(item: GenericGroupItemModel) => (
        <TableRow data-cy="activity-group-generic-item">
          <TableCell>{item.entityClass}</TableCell>
          <TableCell>{describe(item)}</TableCell>
          <TableCell>{modificationSummary(item)}</TableCell>
        </TableRow>
      )}
    />
  );
};

const ItemsTable: FC<React.PropsWithChildren> = (props) => (
  <Table size="small">
    <TableHead>
      <TableRow>
        <TableCell />
        <TableCell />
        <TableCell />
      </TableRow>
    </TableHead>
    <TableBody>{props.children}</TableBody>
  </Table>
);

function describe(item: GenericGroupItemModel): string {
  for (const field of DISPLAYED_DESCRIPTION_FIELDS) {
    const value = item.description?.[field];
    if (typeof value === 'string' && value) {
      return value;
    }
  }
  return `#${item.entityId}`;
}

function modificationSummary(item: GenericGroupItemModel): string {
  const mods = item.modifications || {};
  return Object.keys(mods).join(', ');
}
