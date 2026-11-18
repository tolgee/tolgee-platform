import { useMemo, useState } from 'react';

import { components } from 'tg.service/apiSchema.generated';

type LanguageStatsModel = components['schemas']['LanguageStatsModel'];

export type CommunityLanguageSort = 'name' | 'nameDesc' | 'workLeft' | 'done';

export const useCommunityLanguageList = (
  languages: LanguageStatsModel[],
  baseLanguageTag: string | undefined,
  myLanguageIds: Set<number>
) => {
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState<CommunityLanguageSort>('name');

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

  return { search, setSearch, sort, setSort, base, hasTargets, targets };
};

export const progressOf = (l: LanguageStatsModel) => {
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
