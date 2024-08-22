import React, { FC } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { CollapsibleActivityGroup } from './CollapsibleActivityGroup';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { PaginatedHateoasList } from '../../../common/list/PaginatedHateoasList';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
} from '@mui/material';
import { T } from '@tolgee/react';

type Group = components['schemas']['ActivityGroupCreateKeyModel'];

export const CreateKeysActivityGroup: FC<{
  group: Group;
}> = ({ group }) => {
  return (
    <CollapsibleActivityGroup
      expandedChildren={<ExpandedContent group={group} />}
    >
      {group.author?.name} Created {group.data?.keyCount} keys{' '}
    </CollapsibleActivityGroup>
  );
};

const ExpandedContent: FC<{ group: Group }> = (props) => {
  const project = useProject();

  const [page, setPage] = React.useState(0);

  const loadable = useApiQuery({
    url: '/v2/projects/{projectId}/activity/group-items/create-key/{groupId}',
    method: 'get',
    path: { projectId: project.id, groupId: props.group.id },
    query: {
      page: page,
      size: 20,
    },
  });
  return (
    <PaginatedHateoasList
      listComponent={TheTable}
      renderItem={(i) => (
        <TableRow>
          <TableCell>{i.name}</TableCell>
        </TableRow>
      )}
      onPageChange={(p) => setPage(p)}
      loadable={loadable}
    />
  );
};

const TheTable: FC = (props) => {
  return (
    <Table>
      <TableHead>
        <TableCell>
          <T keyName="activity_group_create_key_item_table_header_key_name" />
        </TableCell>
      </TableHead>
      <TableBody>{props.children}</TableBody>
    </Table>
  );
};
