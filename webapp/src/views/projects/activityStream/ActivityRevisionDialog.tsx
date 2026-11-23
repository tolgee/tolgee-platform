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
import { T, useTranslate } from '@tolgee/react';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { TRANSLATION_STATES } from 'tg.constants/translationStates';
import { ProjectActivityModel } from 'tg.component/activity/types';
import { components } from 'tg.service/apiSchema.generated';
import { useActivityPhrase } from './useActivityPhrase';
import { ActivityKeyCreatedChanges } from './ActivityKeyCreatedChanges';
import {
  ActivityTranslationChanges,
  isTranslationOnly,
} from './ActivityTranslationChanges';

const StyledTable = styled(Box)`
  display: grid;
  grid-template-columns:
    minmax(140px, 1fr) minmax(100px, auto) minmax(120px, 1.2fr)
    minmax(120px, 1.2fr);
  border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  margin-top: 8px;

  && > :last-child > * {
    border-bottom: none;
  }
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
  translation?: {
    old: { text?: unknown; state?: unknown };
    new: { text?: unknown; state?: unknown };
  };
};

const NOISE_FIELDS = [
  'characterCount',
  'qaChecksStale',
  'auto',
  'mtProvider',
  'branch',
];

const isEmpty = (value: unknown) =>
  value === null ||
  value === undefined ||
  value === '' ||
  value === false ||
  (Array.isArray(value) && value.length === 0);

const TranslationValue = ({
  text,
  state,
}: {
  text?: unknown;
  state?: unknown;
}) => {
  const config =
    TRANSLATION_STATES[state as keyof typeof TRANSLATION_STATES] ?? undefined;
  if (isEmpty(text) && !config) return <span>—</span>;
  return (
    <Box display="flex" alignItems="stretch">
      {config && (
        <StyledStateBar
          style={{ background: config.color, height: 'auto', minHeight: 14 }}
        />
      )}
      <span>{isEmpty(text) ? config?.translation : str(text)}</span>
    </Box>
  );
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
    const all = (entity.modifications ?? {}) as Record<
      string,
      { old?: unknown; new?: unknown }
    >;
    const modifications = Object.entries(all).filter(([field, change]) => {
      if (NOISE_FIELDS.includes(field)) return false;
      if (isEmpty(change.old) && isEmpty(change.new)) return false;
      if (field === 'outdated') {
        return data.type === 'SET_OUTDATED_FLAG' || change.new === true;
      }
      if (field === 'isPlural' && isEmpty(change.old)) return false;
      return true;
    });
    const result: Row[] = [];
    if (
      entity.entityClass === 'Translation' &&
      (all.text !== undefined || all.state !== undefined)
    ) {
      const description = (entity.description ?? {}) as Record<string, unknown>;
      result.push({
        entity,
        translation: {
          old: {
            text: all.text ? all.text.old : description.text,
            state: all.state?.old,
          },
          new: {
            text: all.text ? all.text.new : description.text,
            state: all.state?.new,
          },
        },
      });
    }
    modifications
      .filter(
        ([field]) =>
          !(
            entity.entityClass === 'Translation' &&
            (field === 'text' || field === 'state')
          )
      )
      .forEach(([field, change]) =>
        result.push({ entity, field, old: change.old, new: change.new })
      );
    return result.length ? result : [{ entity }];
  });

  const changedRows = rows.filter((row) => row.field || row.translation);
  const visibleRows = changedRows.length ? changedRows : rows;

  if (data.type === 'CREATE_KEY' && data.modifiedEntities?.Key?.length) {
    return (
      <ActivityKeyCreatedChanges
        entities={data.modifiedEntities as Record<string, Entity[]>}
      />
    );
  }

  if (isTranslationOnly(entities)) {
    return <ActivityTranslationChanges entities={entities} />;
  }

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
                <Box>
                  {row.translation ? (
                    <T
                      keyName="activity_revision_field_translation"
                      defaultValue="Translation"
                    />
                  ) : row.field ? (
                    humanize(row.field)
                  ) : (
                    '—'
                  )}
                </Box>
                <StyledPrevious>
                  {row.translation ? (
                    <TranslationValue {...row.translation.old} />
                  ) : row.field ? (
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
                  {row.translation ? (
                    <TranslationValue {...row.translation.new} />
                  ) : row.field ? (
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

const StyledHeader = styled(Box)`
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 400;

  & b {
    font-weight: 600;
  }
`;

const StyledHeaderKey = styled('span')`
  color: ${({ theme }) => theme.palette.primary.main};
`;

const headerObject = (data: ProjectActivityModel) => {
  const names = new Set<string>();
  const walk = (node: unknown) => {
    if (!node || typeof node !== 'object') return;
    const n = node as Record<string, unknown>;
    const relations = n.relations as Record<string, Record<string, unknown>>;
    Object.entries(relations ?? {}).forEach(([name, rel]) => {
      const data = rel?.data as { name?: string } | undefined;
      if (name === 'key' && data?.name) names.add(data.name);
      walk(rel);
    });
  };
  Object.entries(
    (data.modifiedEntities ?? {}) as Record<string, Entity[]>
  ).forEach(([entityClass, list]) =>
    list.forEach((entity) => {
      if (entityClass === 'Key') {
        const description = (entity.description ?? {}) as { name?: string };
        const name =
          description.name ??
          (entity.modifications?.name?.new as string | undefined) ??
          (entity.modifications?.name?.old as string | undefined);
        if (name) names.add(name);
      }
      walk(entity);
    })
  );
  return names.size === 1 ? [...names][0] : undefined;
};

const StyledWhen = styled(Box)`
  margin-left: auto;
  font-size: 13px;
  white-space: nowrap;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

export const ActivityRevisionDialog: FunctionComponent<Props> = ({
  data,
  onClose,
}) => {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();
  const getPhrase = useActivityPhrase();
  const author = data.author;
  return (
    <Dialog
      open
      onClose={onClose}
      maxWidth="lg"
      fullWidth
      data-cy="activity-revision-dialog"
    >
      <DialogTitle sx={{ pt: 3, pb: 2 }}>
        <StyledHeader data-cy="activity-revision-dialog-header">
          {author && (
            <AvatarImg
              size={32}
              owner={{
                type: 'USER',
                id: author.id,
                avatar: author.avatar,
                deleted: author.deleted,
              }}
            />
          )}
          <Box>
            <T
              keyName="activity_revision_dialog_header"
              defaultValue="<b>{name}</b> {action} <key>{object}</key>"
              params={{
                b: <b />,
                key: <StyledHeaderKey />,
                object: headerObject(data) ?? '',
                name:
                  author?.name ||
                  t('activity_group_unknown_author', 'Unknown user'),
                action: getPhrase(data) ?? data.type,
              }}
            />
          </Box>
          <StyledWhen>
            {formatDate(data.timestamp, {
              dateStyle: 'medium',
              timeStyle: 'short',
            })}
          </StyledWhen>
        </StyledHeader>
      </DialogTitle>
      <DialogContent sx={{ pb: 1, '& > *': { mx: -1 } }}>
        <ActivityRevisionChanges data={data} />
      </DialogContent>
      <DialogActions sx={{ px: 3, pt: 2, pb: 3 }}>
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
