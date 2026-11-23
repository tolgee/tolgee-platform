import { FunctionComponent, useMemo } from 'react';
import { Box, styled, Tooltip } from '@mui/material';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { useTranslate } from '@tolgee/react';
import { buildActivity } from 'tg.component/activity/activityTools';
import { ActivityTitle } from 'tg.component/activity/ActivityTitle';
import {
  ActivityModel,
  ProjectActivityModel,
} from 'tg.component/activity/types';
import { ActivityRowShell } from './ActivityRowShell';
import { useActivityPhrase } from './useActivityPhrase';

const StyledTitle = styled(Box)`
  display: contents;

  && > div {
    display: contents;
  }

  && > div > div:nth-of-type(2) {
    flex-basis: 100%;
    min-width: 0;
  }

  &.lowercase > div > div:first-of-type::first-letter {
    text-transform: lowercase;
  }

  && .reference {
    background: transparent;
    border: none;
    padding: 0px;
    max-height: none;
  }

  && .reference:not(.referenceLink) {
    color: ${({ theme }) => theme.palette.text.primary};
    font-weight: 500;
  }

  && .referenceComposed {
    gap: 5px;
  }

  && > div,
  && .reference,
  && .referenceComposed {
    align-items: baseline;
  }

  && .reference span:has(img) {
    align-self: center;
  }

  && .reference div:has(> img) {
    width: auto !important;
    height: auto !important;
    border-radius: 0px;
    overflow: visible;
    filter: none;
  }

  && .reference img {
    position: static;
    width: auto;
    height: 16px;
  }
`;

const StyledObject = styled(Box)`
  flex-basis: 100%;
  display: flex;
  align-items: center;
  gap: 5px;
  color: ${({ theme }) => theme.palette.primary.main};
`;

const StyledFlag = styled('span')`
  display: inline-flex;
  gap: 5px;

  &.muted {
    filter: grayscale(1);
    opacity: 0.45;
  }
`;

type OutdatedLanguage = {
  tag: string;
  name: string;
  flagEmoji: string;
  outdatedOnly: boolean;
};

const getOutdatedObject = (data: ProjectActivityModel) => {
  if (data.type !== 'SET_TRANSLATIONS') return undefined;
  const translations = ((
    data.modifiedEntities as Record<string, unknown[]> | undefined
  )?.Translation ?? []) as {
    modifications?: Record<string, { new?: unknown }>;
    relations?: Record<string, RelationNode>;
  }[];
  const languages: OutdatedLanguage[] = translations.map((e) => {
    const lang = (e.relations?.language?.data ?? {}) as Record<string, string>;
    return {
      tag: lang.tag,
      name: lang.name ?? lang.tag,
      flagEmoji: lang.flagEmoji,
      outdatedOnly:
        e.modifications?.outdated?.new === true && !e.modifications?.text,
    };
  });
  if (!languages.some((l) => l.outdatedOnly)) return undefined;
  const key = translations[0]?.relations?.key?.data?.name;
  return {
    key: String(key ?? ''),
    languages: languages.sort(
      (a, b) => Number(a.outdatedOnly) - Number(b.outdatedOnly)
    ),
  };
};

type RelationNode = {
  data?: Record<string, unknown>;
  relations?: Record<string, RelationNode>;
};

const OWN_OBJECT_TYPES: string[] = [
  'KEY_SOFT_DELETE',
  'KEY_HARD_DELETE',
  'BATCH_KEY_HARD_DELETE',
  'KEY_RESTORE',
  'BATCH_KEY_RESTORE',
  'BATCH_TAG_KEYS',
  'BATCH_UNTAG_KEYS',
  'COMPLEX_TAG_OPERATION',
  'BATCH_SET_KEYS_NAMESPACE',
];

type Props = {
  data: ProjectActivityModel;
  diffEnabled: boolean;
  onOpen?: (data: ActivityModel) => void;
  expanded?: boolean;
};

export const ActivityRevisionRow: FunctionComponent<Props> = ({
  data,
  diffEnabled,
  onOpen,
  expanded,
}) => {
  const { t } = useTranslate();
  const getPhrase = useActivityPhrase();
  const phrase = getPhrase(data);
  const outdatedObject = useMemo(() => getOutdatedObject(data), [data]);
  const activity = useMemo(() => {
    const built =
      OWN_OBJECT_TYPES.includes(data.type) || outdatedObject
        ? { ...buildActivity(data, true), references: [] }
        : buildActivity(data, true);
    return phrase ? { ...built, translation: () => <>{phrase}</> } : built;
  }, [data, phrase]);

  const fallbackObject = useMemo(() => {
    const ownObject = OWN_OBJECT_TYPES.includes(data.type);
    if (activity.references.length && !ownObject) return undefined;
    const entities = (data.modifiedEntities ?? {}) as Record<
      string,
      {
        description?: Record<string, unknown>;
        modifications?: Record<string, { old?: unknown; new?: unknown }>;
        relations?: Record<string, RelationNode>;
      }[]
    >;
    const namespace = entities.Namespace?.[0]?.modifications?.name;
    if (namespace?.old && namespace?.new && !entities.Key) {
      return `${namespace.old} → ${namespace.new}`;
    }
    const names = new Set<string>();
    entities.Key?.forEach((e) => {
      const name =
        e.description?.name ??
        e.modifications?.name?.old ??
        e.modifications?.name?.new;
      if (name) names.add(String(name));
    });
    const collectKeys = (relations?: Record<string, RelationNode>) => {
      Object.entries(relations ?? {}).forEach(([name, relation]) => {
        if (name === 'key' && relation.data?.name) {
          names.add(String(relation.data.name));
        }
        collectKeys(relation.relations);
      });
    };
    Object.values(entities)
      .flat()
      .forEach((e) => collectKeys(e.relations as Record<string, RelationNode>));
    const counts = (data.counts ?? {}) as Record<string, number>;
    const total = Math.max(names.size, counts.Key ?? counts.KeyMeta ?? 0);
    if (names.size) {
      const shown = [...names].slice(0, 2).join(', ');
      return total > 2
        ? t('activity_object_keys_more', '{keys} +{count} more', {
            keys: shown,
            count: total - 2,
          })
        : shown;
    }
    const named = Object.values(entities)
      .flat()
      .map(
        (e) =>
          e.description?.name ??
          e.modifications?.name?.new ??
          e.modifications?.name?.old
      )
      .find(Boolean);
    if (!total && named) {
      return String(named);
    }
    if (total) {
      return t(
        'activity_object_key_count',
        '{count, plural, one {# key} other {# keys}}',
        { count: total }
      );
    }
    return undefined;
  }, [activity, data]);

  return (
    <>
      <ActivityRowShell
        data-cy="activity-revision-row"
        data-cy-type={data.type}
        author={data.author}
        timestamp={data.timestamp}
        onClick={onOpen && (() => onOpen(data as ActivityModel))}
        chevron={expanded === undefined ? 'right' : expanded ? 'up' : 'down'}
        singleLine={
          activity.references.length === 0 && !fallbackObject && !outdatedObject
        }
        title={
          <>
            <b>
              {data.author?.name ||
                t('activity_group_unknown_author', 'Unknown user')}
            </b>
            <StyledTitle
              className={activity.translation ? 'lowercase' : undefined}
            >
              <ActivityTitle activity={activity} />
            </StyledTitle>
            {outdatedObject && (
              <StyledObject data-cy="activity-revision-object">
                <span>{outdatedObject.key}</span>
                {outdatedObject.languages
                  .filter((lang) => !lang.outdatedOnly)
                  .map((lang) => (
                    <Tooltip
                      key={lang.tag}
                      title={lang.name}
                      disableInteractive
                    >
                      <StyledFlag>
                        <FlagImage flagEmoji={lang.flagEmoji} height={14} />
                      </StyledFlag>
                    </Tooltip>
                  ))}
                <Tooltip
                  title={t('activity_flags_outdated', 'Outdated: {languages}', {
                    languages: outdatedObject.languages
                      .filter((lang) => lang.outdatedOnly)
                      .map((lang) => lang.name)
                      .join(', '),
                  })}
                  disableInteractive
                >
                  <StyledFlag className="muted">
                    {outdatedObject.languages
                      .filter((lang) => lang.outdatedOnly)
                      .map((lang) => (
                        <FlagImage
                          key={lang.tag}
                          flagEmoji={lang.flagEmoji}
                          height={14}
                        />
                      ))}
                  </StyledFlag>
                </Tooltip>
              </StyledObject>
            )}
            {!outdatedObject && fallbackObject && (
              <Box
                flexBasis="100%"
                color="primary.main"
                data-cy="activity-revision-object"
              >
                {fallbackObject}
              </Box>
            )}
          </>
        }
      />
    </>
  );
};
