import { FunctionComponent } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  styled,
} from '@mui/material';
import { T } from '@tolgee/react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { TRANSLATION_STATES } from 'tg.constants/translationStates';
import { ProjectActivityModel } from 'tg.component/activity/types';
import { components } from 'tg.service/apiSchema.generated';
import { ActivityRevisionRow } from './ActivityRevisionRow';

const StyledTable = styled(Box)`
  display: grid;
  grid-template-columns:
    minmax(140px, 1fr) minmax(100px, auto) minmax(120px, 1.2fr)
    minmax(120px, 1.2fr);
  border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  margin-top: 8px;
`;

const StyledRow = styled(Box)`
  display: contents;

  & > * {
    padding: 10px 8px;
    border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
    font-size: 14px;
    min-width: 0;
    overflow-wrap: anywhere;
  }
`;

const StyledHead = styled(StyledRow)`
  & > * {
    font-size: 12px;
    font-weight: 500;
    color: ${({ theme }) => theme.palette.text.secondary};
  }
`;

const StyledItem = styled(Box)`
  display: flex;
  gap: 8px;
  align-items: center;
  color: ${({ theme }) => theme.palette.primary.main};
`;

const StyledPrevious = styled(Box)`
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledStateBar = styled('span')`
  display: inline-block;
  width: 3px;
  height: 14px;
  border-radius: 2px;
  margin-right: 6px;
  vertical-align: middle;
`;

type Entity = components['schemas']['ModifiedEntityModel'];

type Row = {
  entity: Entity;
  field?: string;
  old?: unknown;
  new?: unknown;
};

const humanize = (value: string) =>
  value
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/^./, (c) => c.toUpperCase());

const isDateField = (field: string) => /At$/.test(field);

const str = (value: unknown, field = ''): string => {
  if (value === null || value === undefined || value === '') return '—';
  if (isDateField(field) && typeof value === 'number') {
    return new Date(value).toLocaleString();
  }
  if (typeof value === 'string' && /^[A-Z_]+$/.test(value)) {
    return humanize(value.toLowerCase().replace(/_/g, ' '));
  }
  if (Array.isArray(value)) return value.map((v) => str(v)).join(', ');
  if (typeof value === 'object') {
    const o = value as Record<string, unknown>;
    const data = (o.data ?? {}) as Record<string, unknown>;
    return String(
      o.name ?? o.tag ?? o.text ?? data.name ?? data.tag ?? JSON.stringify(o)
    );
  }
  if (typeof value === 'boolean') return value ? 'Yes' : 'No';
  return String(value);
};

const Value = ({
  field,
  value,
  mentions,
}: {
  field: string;
  value: unknown;
  mentions: Record<string, string>;
}) => {
  if (typeof value === 'string' && value.includes('<@')) {
    return (
      <span>
        {value.replace(/<@(\d+)>/g, (_, id) => `@${mentions[id] ?? id}`)}
      </span>
    );
  }
  if (field === 'state') {
    const config = TRANSLATION_STATES[value as keyof typeof TRANSLATION_STATES];
    if (config) {
      return (
        <span>
          <StyledStateBar style={{ background: config.color }} />
          {config.translation}
        </span>
      );
    }
  }
  return <span>{str(value, field)}</span>;
};

const entityName = (entity: Entity) => {
  const key = entity.relations?.key?.data;
  const description = (entity.description ?? {}) as Record<string, unknown>;
  const name =
    key?.name ??
    description.name ??
    description.tag ??
    entity.modifications?.name?.new ??
    entity.modifications?.name?.old;
  return name
    ? str(name)
    : humanize(entity.entityClass).replace(/ Config$/, '');
};

type Props = {
  data: ProjectActivityModel;
  onClose: () => void;
};

export const ActivityRevisionChanges: FunctionComponent<{
  data: ProjectActivityModel;
}> = ({ data }) => {
  const mentions: Record<string, string> = {};
  const entities: Entity[] = Object.values(
    (data.modifiedEntities ?? {}) as Record<string, Entity[]>
  ).flat();
  const rows: Row[] = entities.flatMap((entity): Row[] => {
    const modifications = Object.entries(entity.modifications ?? {}).filter(
      ([field]) => !['characterCount', 'qaChecksStale', 'auto'].includes(field)
    );
    if (!modifications.length) {
      return [{ entity }];
    }
    return modifications.map(([field, change]) => ({
      entity,
      field,
      old: change.old as unknown,
      new: change.new as unknown,
    }));
  });

  const changedRows = rows.filter((row) => row.field);
  const visibleRows = changedRows.length ? changedRows : rows;

  return (
    <>
      {visibleRows.length > 0 && (
        <StyledTable data-cy="activity-revision-changes">
          <StyledHead>
            <Box>
              <T keyName="activity_revision_item" defaultValue="Item" />
            </Box>
            <Box>
              <T keyName="activity_revision_field" defaultValue="Field" />
            </Box>
            <Box>
              <T keyName="activity_revision_previous" defaultValue="Previous" />
            </Box>
            <Box>
              <T keyName="activity_revision_current" defaultValue="Current" />
            </Box>
          </StyledHead>
          {visibleRows.map((row, index) => {
            const language = row.entity.relations?.language?.data as
              | { flagEmoji?: string; tag?: string }
              | undefined;
            return (
              <StyledRow key={index} data-cy="activity-revision-change">
                <StyledItem>
                  {language?.flagEmoji && (
                    <FlagImage flagEmoji={language.flagEmoji} height={14} />
                  )}
                  {entityName(row.entity)}
                </StyledItem>
                <Box>{row.field ? humanize(row.field) : '—'}</Box>
                <StyledPrevious>
                  {row.field ? (
                    <Value
                      field={row.field}
                      value={row.old}
                      mentions={mentions}
                    />
                  ) : (
                    '—'
                  )}
                </StyledPrevious>
                <Box>
                  {row.field ? (
                    <Value
                      field={row.field}
                      value={row.new}
                      mentions={mentions}
                    />
                  ) : (
                    '—'
                  )}
                </Box>
              </StyledRow>
            );
          })}
        </StyledTable>
      )}
    </>
  );
};

export const ActivityRevisionDialog: FunctionComponent<Props> = ({
  data,
  onClose,
}) => {
  return (
    <Dialog
      open
      onClose={onClose}
      maxWidth="md"
      fullWidth
      data-cy="activity-revision-dialog"
    >
      <DialogTitle>
        <T keyName="activity_revision_dialog_title" defaultValue="Activity" />
      </DialogTitle>
      <DialogContent>
        <ActivityRevisionRow data={data} diffEnabled />
        <ActivityRevisionChanges data={data} />
      </DialogContent>
      <DialogActions>
        <Button
          variant="contained"
          color="primary"
          onClick={onClose}
          data-cy="activity-revision-dialog-close"
        >
          <T keyName="global_close_button" />
        </Button>
      </DialogActions>
    </Dialog>
  );
};
