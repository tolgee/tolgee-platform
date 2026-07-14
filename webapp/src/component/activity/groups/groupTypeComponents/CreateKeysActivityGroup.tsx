import React, { FC, useState } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
} from '@mui/material';
import { T } from '@tolgee/react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { components } from 'tg.service/apiSchema.generated';

type CreateKeyGroupItemModel = components['schemas']['CreateKeyGroupItemModel'];

export const CreateKeysExpandedContent: FC<{ groupId: number }> = (props) => {
  const project = useProject();
  const [page, setPage] = useState(0);

  const loadable = useApiQuery({
    url: '/v2/projects/{projectId}/activity/group-items/create-key/{groupId}',
    method: 'get',
    path: { projectId: project.id, groupId: props.groupId },
    query: { page, size: 20 },
  });

  return (
    <PaginatedHateoasList
      loadable={loadable}
      onPageChange={(p) => setPage(p)}
      listComponent={KeysTable}
      renderItem={(item: CreateKeyGroupItemModel) => (
        <TableRow data-cy="activity-group-create-key-item">
          <TableCell>{item.name}</TableCell>
          <TableCell>{item.baseTranslationText ?? ''}</TableCell>
        </TableRow>
      )}
    />
  );
};

const KeysTable: FC<React.PropsWithChildren> = (props) => (
  <Table size="small">
    <TableHead>
      <TableRow>
        <TableCell>
          <T keyName="activity_groups_key_name_column" defaultValue="Key" />
        </TableCell>
        <TableCell>
          <T
            keyName="activity_groups_base_translation_column"
            defaultValue="Base translation"
          />
        </TableCell>
      </TableRow>
    </TableHead>
    <TableBody>{props.children}</TableBody>
  </Table>
);
