import { FunctionComponent } from 'react';
import { alpha, Box, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { TRANSLATION_STATES } from 'tg.constants/translationStates';
import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';

type Entity = components['schemas']['ModifiedEntityModel'];
type Change = { old?: unknown; new?: unknown };
type Language = { tag: string; name?: string; flagEmoji?: string };
type Cell = {
  text?: unknown;
  oldText?: unknown;
  state?: unknown;
  changed: boolean;
  outdated?: boolean;
  acceptedFrom?: string;
};

const PROVIDER_NAMES: Record<string, string> = {
  PROMPT: 'AI',
  GOOGLE: 'Google',
  DEEPL: 'DeepL',
  AWS: 'Amazon Translate',
  AZURE: 'Azure',
  BAIDU: 'Baidu',
};

const StyledTable = styled(Box)`
  display: grid;
  margin-top: 8px;
  font-size: 14px;

  && > :last-child > * {
    border-bottom: none;
  }
`;

const StyledHeadCell = styled(Box)`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 8px;
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.secondary};
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};

  &:first-of-type {
    color: ${({ theme }) => theme.palette.text.primary};
    font-weight: 500;
  }
`;

const StyledLanguage = styled(Box)`
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px;
  font-size: 13px;
  white-space: nowrap;
  color: ${({ theme }) => theme.palette.text.secondary};
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};

  & img {
    margin-top: 2px;
  }
`;

const StyledCell = styled(Box)`
  padding: 8px;
  min-width: 0;
  overflow-wrap: anywhere;
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
`;

const StyledKey = styled(StyledCell)`
  color: ${({ theme }) => theme.palette.primary.main};
`;

const StyledText = styled(Box)`
  display: flex;
  gap: 10px;
  align-items: stretch;
`;

const StyledBar = styled('span')`
  flex: 0 0 3px;
  border-radius: 2px;
`;

const StyledNew = styled('span')`
  background: ${({ theme }) => alpha(theme.palette.success.main, 0.12)};
  border-radius: 2px;
  padding: 0 2px;
`;

const StyledOld = styled('div')`
  color: ${({ theme }) => theme.palette.text.secondary};
  text-decoration: line-through;
  margin-bottom: 2px;
`;

const StyledOutdated = styled('span')`
  display: inline-block;
  margin-bottom: 4px;
  padding: 0 6px;
  border-radius: 10px;
  font-size: 12px;
  color: ${({ theme }) => theme.palette.warning.dark};
  background: ${({ theme }) => alpha(theme.palette.warning.main, 0.15)};
`;

const StyledAccepted = styled('span')`
  display: inline-block;
  margin-bottom: 4px;
  padding: 0 6px;
  border-radius: 10px;
  font-size: 12px;
  color: ${({ theme }) => theme.palette.success.dark};
  background: ${({ theme }) => alpha(theme.palette.success.main, 0.15)};
`;

const TranslationCell = ({ cell }: { cell?: Cell }) => {
  if (!cell) return <StyledCell>—</StyledCell>;
  const color =
    TRANSLATION_STATES[cell.state as keyof typeof TRANSLATION_STATES]?.color;
  const text = typeof cell.text === 'string' ? cell.text : '';
  const oldText = typeof cell.oldText === 'string' ? cell.oldText : '';
  return (
    <StyledCell>
      <StyledText>
        <StyledBar style={{ background: color ?? 'transparent' }} />
        <Box minWidth={0}>
          {cell.acceptedFrom !== undefined && (
            <Box>
              <StyledAccepted>
                <T
                  keyName="activity_translation_mt_accepted"
                  defaultValue="Machine translation accepted{provider, select, none {} other { · {provider}}}"
                  params={{ provider: cell.acceptedFrom || 'none' }}
                />
              </StyledAccepted>
            </Box>
          )}
          {cell.outdated && (
            <Box>
              <StyledOutdated>
                <T
                  keyName="activity_translation_outdated"
                  defaultValue="Outdated"
                />
              </StyledOutdated>
            </Box>
          )}
          {cell.changed && oldText && oldText !== text && (
            <StyledOld>{oldText}</StyledOld>
          )}
          {cell.changed ? <StyledNew>{text || '—'}</StyledNew> : text || '—'}
        </Box>
      </StyledText>
    </StyledCell>
  );
};

export const isTranslationOnly = (entities: Entity[]) =>
  entities.length > 0 &&
  entities.every(
    (e) =>
      e.entityClass === 'Translation' &&
      e.relations?.key?.data &&
      e.relations?.language?.data &&
      ((e.modifications as Record<string, Change>)?.text ||
        (e.modifications as Record<string, Change>)?.state ||
        (e.modifications as Record<string, Change>)?.outdated?.new === true ||
        (e.modifications as Record<string, Change>)?.auto?.old === true)
  );

export const ActivityTranslationChanges: FunctionComponent<{
  entities: Entity[];
}> = ({ entities }) => {
  const project = useProject();
  const base = project.baseLanguage as Language | undefined;

  const keys = new Map<number, string>();
  const languages = new Map<string, Language>();
  const cells = new Map<string, Cell>();
  entities.forEach((entity) => {
    const keyId = entity.relations!.key!.entityId as number;
    const lang = entity.relations!.language!.data as unknown as Language;
    const mods = (entity.modifications ?? {}) as Record<string, Change>;
    const description = (entity.description ?? {}) as Record<string, unknown>;
    keys.set(keyId, String(entity.relations!.key!.data!.name));
    languages.set(lang.tag, lang);
    cells.set(`${keyId}:${lang.tag}`, {
      text: mods.text ? mods.text.new : description.text,
      oldText: mods.text?.old,
      state: mods.state?.new,
      changed: Boolean(
        mods.text || (mods.state && mods.outdated?.new !== true)
      ),
      outdated: mods.outdated?.new === true,
      acceptedFrom:
        mods.auto?.old === true && mods.auto?.new === false && !mods.text
          ? PROVIDER_NAMES[String(mods.mtProvider?.old)] ??
            (mods.mtProvider?.old ? String(mods.mtProvider.old) : '')
          : undefined,
    });
  });

  const keyIds = [...keys.keys()];
  const tags = [...new Set([...(base ? [base.tag] : []), ...languages.keys()])];
  const current = useApiQuery({
    url: '/v2/projects/{projectId}/translations',
    method: 'get',
    path: { projectId: project.id },
    query: {
      filterKeyId: keyIds,
      languages: tags,
      size: keyIds.length,
    },
  });
  current.data?._embedded?.keys?.forEach((key) => {
    tags.forEach((tag) => {
      const translation = key.translations?.[tag];
      const id = `${key.keyId}:${tag}`;
      const cell = cells.get(id);
      if (!cell) {
        cells.set(id, {
          text: translation?.text,
          state: translation?.state,
          changed: false,
        });
        return;
      }
      if (cell.text === undefined && !cell.changed) {
        cell.text = translation?.text;
      }
      if (cell.state === undefined) {
        cell.state = translation?.state;
      }
    });
  });

  const columns: Language[] = [
    ...(base ? [languages.get(base.tag) ?? base] : []),
    ...[...languages.values()].filter((l) => l.tag !== base?.tag),
  ];

  const showKey = keyIds.length > 1;

  if (!showKey) {
    const keyId = keyIds[0];
    return (
      <StyledTable
        data-cy="activity-translation-changes"
        sx={{ gridTemplateColumns: 'minmax(140px, auto) 1fr' }}
      >
        {columns.map((lang) => (
          <Box key={lang.tag} display="contents">
            <StyledLanguage>
              {lang.flagEmoji && (
                <FlagImage flagEmoji={lang.flagEmoji} height={16} />
              )}
              {lang.name ?? lang.tag}
            </StyledLanguage>
            <TranslationCell cell={cells.get(`${keyId}:${lang.tag}`)} />
          </Box>
        ))}
      </StyledTable>
    );
  }

  return (
    <StyledTable
      data-cy="activity-translation-changes"
      sx={{
        gridTemplateColumns: `${showKey ? 'minmax(120px, 0.8fr) ' : ''}${columns
          .map(() => 'minmax(140px, 1.5fr)')
          .join(' ')}`,
      }}
    >
      {showKey && (
        <StyledHeadCell>
          <T keyName="activity_revision_key" defaultValue="Key" />
        </StyledHeadCell>
      )}
      {columns.map((lang) => (
        <StyledHeadCell key={lang.tag}>
          {lang.flagEmoji && (
            <FlagImage flagEmoji={lang.flagEmoji} height={16} />
          )}
          {lang.name ?? lang.tag}
        </StyledHeadCell>
      ))}
      {keyIds.map((keyId) => (
        <Box key={keyId} display="contents">
          {showKey && <StyledKey>{keys.get(keyId)}</StyledKey>}
          {columns.map((lang) => (
            <TranslationCell
              key={lang.tag}
              cell={cells.get(`${keyId}:${lang.tag}`)}
            />
          ))}
        </Box>
      ))}
    </StyledTable>
  );
};
