import React, { FC, useState } from 'react';
import {
  styled,
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

type GenericGroupItemModel = components['schemas']['GenericGroupItemModel'];

const StyledOld = styled('span')`
  color: ${({ theme }) => theme.palette.text.secondary};
  text-decoration: line-through;
`;

const StyledNew = styled('span')`
  color: ${({ theme }) => theme.palette.text.primary};
`;

const StyledEntity = styled('span')`
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledLanguage = styled('span')`
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const MAX_DISPLAYED_MODIFICATIONS = 3;
const MAX_VALUE_LENGTH = 60;

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
          <TableCell>
            <StyledEntity>{item.entityClass}</StyledEntity>
          </TableCell>
          <TableCell>
            {getTitle(item)}
            {getLanguageTag(item) && (
              <StyledLanguage> ({getLanguageTag(item)})</StyledLanguage>
            )}
          </TableCell>
          <TableCell>
            <Modifications item={item} />
          </TableCell>
        </TableRow>
      )}
    />
  );
};

const ItemsTable: FC<React.PropsWithChildren> = (props) => (
  <Table size="small">
    <TableHead>
      <TableRow>
        <TableCell>
          <T keyName="activity_groups_entity_column" defaultValue="Entity" />
        </TableCell>
        <TableCell>
          <T keyName="activity_groups_item_column" defaultValue="Item" />
        </TableCell>
        <TableCell>
          <T keyName="activity_groups_change_column" defaultValue="Change" />
        </TableCell>
      </TableRow>
    </TableHead>
    <TableBody>{props.children}</TableBody>
  </Table>
);

const Modifications: FC<{ item: GenericGroupItemModel }> = ({ item }) => {
  const modifications = Object.entries(item.modifications || {});
  const displayed = modifications.slice(0, MAX_DISPLAYED_MODIFICATIONS);

  return (
    <>
      {displayed.map(([field, modification], index) => (
        <React.Fragment key={field}>
          {index > 0 && ', '}
          <span>{field}: </span>
          {modification.old !== undefined && modification.old !== null && (
            <>
              <StyledOld>{formatValue(field, modification.old)}</StyledOld>
              {' → '}
            </>
          )}
          <StyledNew>{formatValue(field, modification.new)}</StyledNew>
        </React.Fragment>
      ))}
      {modifications.length > displayed.length &&
        ` +${modifications.length - displayed.length}`}
    </>
  );
};

const TITLE_FIELDS = ['name', 'text', 'tag', 'username', 'url'];

function getTitle(item: GenericGroupItemModel): string {
  // a translation is best identified by the key it belongs to
  const keyName = item.relations?.key?.data?.name;
  if (typeof keyName === 'string' && keyName) {
    return keyName;
  }

  for (const field of TITLE_FIELDS) {
    const described = item.description?.[field];
    if (typeof described === 'string' && described) {
      return described;
    }
    const modification = item.modifications?.[field];
    for (const value of [modification?.new, modification?.old]) {
      if (typeof value === 'string' && value) {
        return value;
      }
    }
  }

  return `#${item.entityId}`;
}

function getLanguageTag(item: GenericGroupItemModel): string | undefined {
  const tag = item.relations?.language?.data?.tag;
  return typeof tag === 'string' ? tag : undefined;
}

const NAME_FIELDS = ['name', 'text', 'tag', 'title'];

function formatValue(field: string, value: unknown): string {
  return truncate(stringifyValue(field, value));
}

function stringifyValue(field: string, value: unknown): string {
  if (value === null || value === undefined) {
    return '∅';
  }

  if (typeof value === 'string') {
    return value;
  }

  if (typeof value === 'number') {
    return isTimestampField(field)
      ? new Date(value).toLocaleString()
      : String(value);
  }

  if (Array.isArray(value)) {
    return value.map((item) => stringifyValue(field, item)).join(', ') || '∅';
  }

  if (typeof value === 'object') {
    // entity references and describing data carry their own name
    const reference = value as Record<string, any>;
    const data = reference.data ?? reference;
    for (const nameField of NAME_FIELDS) {
      const name = data?.[nameField];
      if (typeof name === 'string' && name) {
        return name;
      }
    }
    if (reference.entityId) {
      return `#${reference.entityId}`;
    }
    return JSON.stringify(value);
  }

  return String(value);
}

function isTimestampField(field: string): boolean {
  return field.endsWith('At') || field.endsWith('Date');
}

function truncate(text: string): string {
  return text.length > MAX_VALUE_LENGTH
    ? `${text.slice(0, MAX_VALUE_LENGTH)}…`
    : text;
}
