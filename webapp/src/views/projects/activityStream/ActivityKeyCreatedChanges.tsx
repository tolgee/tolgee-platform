import { FunctionComponent } from 'react';
import { Box, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { useProject } from 'tg.hooks/useProject';
import { Tag } from 'tg.views/projects/translations/Tags/Tag';
import { components } from 'tg.service/apiSchema.generated';

type Entity = components['schemas']['ModifiedEntityModel'];
type Change = { old?: unknown; new?: unknown };

const StyledTable = styled(Box)`
  display: grid;
  margin-top: 8px;
  font-size: 14px;

  && > :last-child > * {
    border-bottom: none;
  }

  & > .head,
  & > .row > * {
    padding: 8px;
    min-width: 0;
    overflow-wrap: anywhere;
    border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
  }
`;

const StyledHead = styled(Box)`
  font-size: 13px;
  font-weight: 500;
`;

const StyledKey = styled(Box)`
  color: ${({ theme }) => theme.palette.primary.main};
`;

const StyledTags = styled(Box)`
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
`;

const StyledNone = styled('span')`
  color: ${({ theme }) => theme.palette.text.disabled};
  font-style: italic;
`;

const None = () => (
  <StyledNone>
    <T keyName="activity_value_none" defaultValue="None" />
  </StyledNone>
);

const mods = (entity?: Entity) =>
  (entity?.modifications ?? {}) as Record<string, Change>;

const nameOf = (value: unknown) => {
  if (!value) return undefined;
  if (typeof value === 'object') {
    const v = value as { name?: unknown; data?: { name?: unknown } };
    return (v.data?.name ?? v.name) as string | undefined;
  }
  return String(value);
};

export const ActivityKeyCreatedChanges: FunctionComponent<{
  entities: Record<string, Entity[]>;
}> = ({ entities }) => {
  const project = useProject();
  const baseTag = project.baseLanguage?.tag;

  const keys = (entities.Key ?? []).map((key) => {
    const keyMods = mods(key);
    const meta = (entities.KeyMeta ?? []).find(
      (m) => m.relations?.key?.entityId === key.entityId
    );
    const base = (entities.Translation ?? []).find(
      (t) =>
        t.relations?.key?.entityId === key.entityId &&
        (t.relations?.language?.data as { tag?: string } | undefined)?.tag ===
          baseTag
    );
    const tags = mods(meta).tags?.new;
    return {
      id: key.entityId,
      name: nameOf(keyMods.name?.new) ?? '',
      description: mods(meta).description?.new as string | undefined,
      baseText: mods(base).text?.new as string | undefined,
      namespace:
        nameOf(keyMods.namespace?.new) ??
        nameOf(key.relations?.namespace?.data),
      tags: Array.isArray(tags) ? (tags as string[]) : [],
    };
  });

  return (
    <StyledTable
      data-cy="activity-key-created-changes"
      sx={{
        gridTemplateColumns: [
          keys.length > 1 && 'minmax(120px, 1fr)',
          keys.some((k) => k.description) ? 'minmax(100px, 1fr)' : 'auto',
          'minmax(140px, 1.6fr)',
          'minmax(90px, auto)',
          'minmax(90px, auto)',
        ]
          .filter(Boolean)
          .join(' '),
      }}
    >
      {keys.length > 1 && (
        <StyledHead className="head">
          <T keyName="activity_revision_key" defaultValue="Key" />
        </StyledHead>
      )}
      <StyledHead className="head">
        <T keyName="activity_revision_description" defaultValue="Description" />
      </StyledHead>
      <StyledHead className="head">
        <T
          keyName="activity_revision_base_language"
          defaultValue="Base language"
        />
      </StyledHead>
      <StyledHead className="head">
        <T keyName="activity_revision_namespace" defaultValue="Namespace" />
      </StyledHead>
      <StyledHead className="head">
        <T keyName="activity_revision_tags" defaultValue="Tags" />
      </StyledHead>
      {keys.map((key) => (
        <Box key={key.id} display="contents" className="row">
          {keys.length > 1 && <StyledKey>{key.name}</StyledKey>}
          <Box>{key.description || <None />}</Box>
          <Box>{key.baseText || <None />}</Box>
          <Box>{key.namespace || <None />}</Box>
          <StyledTags>
            {key.tags.length ? (
              key.tags.map((tag) => <Tag key={tag} name={tag} />)
            ) : (
              <None />
            )}
          </StyledTags>
        </Box>
      ))}
    </StyledTable>
  );
};
