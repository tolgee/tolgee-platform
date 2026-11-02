import { useMemo, useState } from 'react';
import {
  Badge,
  Box,
  Chip,
  IconButton,
  styled,
  Tooltip,
  Typography,
  useTheme,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';
import { Star01, Translate01 } from '@untitled-ui/icons-react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';

import { components } from 'tg.service/apiSchema.generated';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { getProjectTranslationsUrl } from 'tg.constants/links';
import { Sort } from 'tg.component/CustomIcons';
import SearchField from 'tg.component/common/form/fields/SearchField';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { TranslationSortMenu } from 'tg.component/translation/translationSort/TranslationSortMenu';
import { TranslationStatesBar } from 'tg.views/projects/TranslationStatesBar';

type LanguageStatsModel = components['schemas']['LanguageStatsModel'];

type SortValue = 'name' | 'nameDesc' | 'workLeft' | 'done';

const progressOf = (l: LanguageStatsModel) => {
  const translated = Number(l.translatedPercentage);
  const reviewed = Number(l.reviewedPercentage);
  if (Number.isFinite(translated) && Number.isFinite(reviewed)) {
    return { translated, reviewed, byWords: true };
  }
  const total =
    l.translatedKeyCount + l.reviewedKeyCount + l.untranslatedKeyCount;
  return {
    translated: total ? (l.translatedKeyCount / total) * 100 : 0,
    reviewed: total ? (l.reviewedKeyCount / total) * 100 : 0,
    byWords: false,
  };
};

const doneOf = (l: LanguageStatsModel) => {
  const { translated, reviewed } = progressOf(l);
  return translated + reviewed;
};

const flagOf = (l: LanguageStatsModel) =>
  l.languageFlagEmoji || languageInfo[l.languageTag!]?.flags?.[0] || '';

const StyledList = styled(Box)`
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-radius: 4px;
  background: ${({ theme }) => theme.palette.background.paper};
  overflow: hidden;
`;

const StyledRow = styled(Box)`
  position: relative;
  display: grid;
  grid-template-columns: minmax(120px, 220px) minmax(160px, 320px) 1fr 30px;
  grid-template-areas: 'name bar left icon';
  gap: ${({ theme }) => theme.spacing(2)};
  align-items: center;
  padding: ${({ theme }) => theme.spacing(3, 2)};
  & + & {
    border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  }
  && .legend {
    font-size: 14px;
  }
  & .translationsButton {
    opacity: 0;
    transition: opacity 0.2s ease-in-out;
  }
  &:hover {
    background: ${({ theme }) => theme.palette.cell.hover};
    & .translationsButton {
      opacity: 1;
    }
  }
  ${({ theme }) => theme.breakpoints.down('sm')} {
    grid-template-columns: 1fr auto;
    grid-template-areas: 'name icon' 'bar bar' 'left left';
    gap: ${({ theme }) => theme.spacing(1, 2)};
    padding: ${({ theme }) => theme.spacing(2)};
    & .translationsButton {
      opacity: 1;
    }
  }
`;

const StyledName = styled(Box)`
  grid-area: name;
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1.5)};
  min-width: 0;
  & a {
    color: inherit;
    text-decoration: none;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  & a::after {
    content: '';
    position: absolute;
    inset: 0;
  }
`;

const StyledWordsLeft = styled(Box)`
  grid-area: left;
  text-align: right;
  white-space: nowrap;
  ${({ theme }) => theme.breakpoints.down('sm')} {
    text-align: left;
    color: ${({ theme }) => theme.palette.text.secondary};
  }
`;

type Props = {
  projectId: number;
  baseLanguageTag?: string;
  languages: LanguageStatsModel[];
  myLanguageIds: Set<number>;
};

export const CommunityLanguages = ({
  projectId,
  baseLanguageTag,
  languages,
  myLanguageIds,
}: Props) => {
  const { t } = useTranslate();
  const theme = useTheme();
  const stickyTop = useGlobalContext(
    (c) => c.layout.topBarHeight + c.layout.topBannerHeight
  );
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState<SortValue>('name');
  const [sortAnchor, setSortAnchor] = useState<HTMLElement | null>(null);

  const sortOptions = [
    { value: 'name', label: t('community_languages_sort_name', 'A to Z') },
    {
      value: 'nameDesc',
      label: t('community_languages_sort_name_desc', 'Z to A'),
    },
    {
      value: 'workLeft',
      label: t('community_languages_sort_work_left', 'Most work left'),
    },
    { value: 'done', label: t('community_languages_sort_done', 'Most done') },
  ];

  const translationsLabel = t(
    'community_languages_translations_button',
    'Translations'
  );
  const base = languages.find((l) => l.languageTag === baseLanguageTag);
  const hasTargets = languages.some((l) => l.languageTag !== baseLanguageTag);

  const targets = useMemo(() => {
    const query = search.trim().toLowerCase();
    const filtered = languages.filter(
      (l) =>
        l.languageTag !== baseLanguageTag &&
        (!query ||
          [l.languageName, l.languageOriginalName, l.languageTag].some((v) =>
            v?.toLowerCase().includes(query)
          ))
    );
    const compare = (a: LanguageStatsModel, b: LanguageStatsModel) => {
      if (sort === 'workLeft') {
        return (
          b.untranslatedWordCount - a.untranslatedWordCount ||
          b.untranslatedKeyCount - a.untranslatedKeyCount
        );
      }
      if (sort === 'done') {
        return doneOf(b) - doneOf(a);
      }
      const byName = (a.languageName || '').localeCompare(b.languageName || '');
      return sort === 'nameDesc' ? -byName : byName;
    };
    return filtered.sort(
      (a, b) =>
        Number(myLanguageIds.has(b.languageId!)) -
          Number(myLanguageIds.has(a.languageId!)) || compare(a, b)
    );
  }, [languages, search, sort, myLanguageIds]);

  const translationsUrl = (tag: string) =>
    getProjectTranslationsUrl(projectId, {
      languages: baseLanguageTag ? [baseLanguageTag, tag] : [tag],
    });

  return (
    <Box display="grid" gap={2} data-cy="community-languages">
      {hasTargets && (
        <Box
          display="flex"
          alignItems="center"
          gap={1}
          position="sticky"
          top={stickyTop}
          zIndex={2}
          py={1}
          my={-1}
          bgcolor="background.default"
          data-cy="community-languages-toolbar"
        >
          <SearchField
            initial={search}
            onSearch={setSearch}
            variant="outlined"
            size="small"
            placeholder={t('community_languages_search', 'Search languages')}
            sx={{ width: { xs: '100%', sm: 250 } }}
          />
          <Tooltip
            title={t('community_languages_sort_tooltip', 'Sort languages')}
          >
            <Badge
              color="primary"
              variant="dot"
              badgeContent={sort === 'name' ? 0 : 1}
              overlap="circular"
            >
              <IconButton
                onClick={(e) => setSortAnchor(e.currentTarget)}
                data-cy="community-languages-sort-button"
              >
                <Sort />
              </IconButton>
            </Badge>
          </Tooltip>
          <TranslationSortMenu
            value={sort}
            options={sortOptions}
            onChange={(value) => setSort(value as SortValue)}
            anchorEl={sortAnchor}
            onClose={() => setSortAnchor(null)}
          />
        </Box>
      )}

      <StyledList>
        {base && !search && (
          <StyledRow data-cy="community-language-base">
            <StyledName>
              <FlagImage flagEmoji={flagOf(base)} height={20} />
              <Box component="span" fontWeight={700} whiteSpace="nowrap">
                {base.languageName}
              </Box>
              <Tooltip
                title={t(
                  'community_languages_base_tooltip',
                  'All translations are made from this language'
                )}
              >
                <Chip
                  size="small"
                  label={t('community_languages_base_label', 'Base')}
                />
              </Tooltip>
            </StyledName>
          </StyledRow>
        )}

        {targets.map((l) => {
          const progress = progressOf(l);
          return (
            <StyledRow key={l.languageId} data-cy="community-language-row">
              <StyledName>
                <FlagImage flagEmoji={flagOf(l)} height={20} />
                <Link to={translationsUrl(l.languageTag!)}>
                  {l.languageName}
                </Link>
                {myLanguageIds.has(l.languageId!) && (
                  <Tooltip
                    title={t(
                      'community_languages_mine_tooltip',
                      "You've contributed here, so it stays on top"
                    )}
                  >
                    <Box
                      display="flex"
                      position="relative"
                      zIndex={1}
                      color="text.secondary"
                      aria-label={t(
                        'community_languages_mine_tooltip',
                        "You've contributed here, so it stays on top"
                      )}
                    >
                      <Star01 width={14} height={14} fill="currentColor" />
                    </Box>
                  </Tooltip>
                )}
              </StyledName>
              <Box gridArea="bar">
                {progress.translated + progress.reviewed === 0 ? (
                  <Tooltip
                    title={t(
                      'community_languages_first_translation_cta',
                      'Add the first translation'
                    )}
                  >
                    <Box
                      data-cy="community-language-not-started"
                      position="relative"
                      zIndex={1}
                    >
                      <TranslationStatesBar
                        labels={false}
                        stats={{
                          keyCount: 1,
                          languageCount: 1,
                          translationStatePercentages: {
                            TRANSLATED: 0,
                            REVIEWED: 0,
                            UNTRANSLATED: 0,
                          },
                        }}
                      />
                      <Box
                        fontSize={13}
                        whiteSpace="nowrap"
                        overflow="hidden"
                        textOverflow="ellipsis"
                      >
                        <Box
                          component="span"
                          className="notStartedStatus"
                          color="text.secondary"
                        >
                          <T
                            keyName="community_languages_nothing_translated"
                            defaultValue="No translations yet"
                          />
                        </Box>
                      </Box>
                    </Box>
                  </Tooltip>
                ) : (
                  <Tooltip
                    title={
                      progress.translated + progress.reviewed >= 100
                        ? ''
                        : t(
                            'community_languages_new_translation_cta',
                            'Add a new translation'
                          )
                    }
                  >
                    <Box position="relative" zIndex={1}>
                      <TranslationStatesBar
                        labels={true}
                        stats={{
                          keyCount: 1,
                          languageCount: 1,
                          translationStatePercentages: {
                            TRANSLATED: progress.translated,
                            REVIEWED: progress.reviewed,
                            UNTRANSLATED: 0,
                          },
                        }}
                      />
                    </Box>
                  </Tooltip>
                )}
              </Box>
              <StyledWordsLeft>
                {progress.byWords ? (
                  <T
                    keyName="community_languages_words_left"
                    defaultValue="{count, plural, one {# word left} other {# words left}}"
                    params={{ count: l.untranslatedWordCount }}
                  />
                ) : (
                  <T
                    keyName="community_languages_keys_left"
                    defaultValue="{count, plural, one {# key left} other {# keys left}}"
                    params={{ count: l.untranslatedKeyCount }}
                  />
                )}
              </StyledWordsLeft>
              <Tooltip title={translationsLabel}>
                <IconButton
                  className="translationsButton"
                  component={Link}
                  to={translationsUrl(l.languageTag!)}
                  size="small"
                  sx={{ gridArea: 'icon', zIndex: 1 }}
                  aria-label={translationsLabel}
                  data-cy="community-language-translations-button"
                >
                  <Translate01 width={20} height={20} />
                </IconButton>
              </Tooltip>
            </StyledRow>
          );
        })}

        {search && targets.length === 0 && (
          <EmptyListMessage height="160px">
            <T
              keyName="community_languages_search_empty"
              defaultValue="No language matches “{search}”"
              params={{ search }}
            />
          </EmptyListMessage>
        )}
        {!search && targets.length === 0 && (
          <Box
            display="flex"
            flexDirection="column"
            alignItems="center"
            gap={3}
            py={6}
            borderTop={base ? 1 : 0}
            borderColor="divider1"
            data-cy="community-languages-empty"
          >
            <Box textAlign="center">
              <Typography sx={{ opacity: 0.8 }}>
                <T
                  keyName="community_languages_empty"
                  defaultValue="No languages to translate yet"
                />
              </Typography>
              <Typography variant="body2" color="text.secondary">
                <T
                  keyName="community_languages_empty_hint"
                  defaultValue="Once the owners add some, they'll appear here."
                />
              </Typography>
            </Box>
            <img
              src={
                theme.palette.mode === 'dark'
                  ? '/images/communityEmptyMouseDark.svg'
                  : '/images/communityEmptyMouseLight.svg'
              }
              alt=""
              draggable={false}
              height={160}
              style={{ opacity: 0.6 }}
            />
          </Box>
        )}
      </StyledList>
    </Box>
  );
};
