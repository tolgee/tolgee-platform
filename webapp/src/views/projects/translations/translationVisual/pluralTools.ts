// Taken from
// https://unicode-org.github.io/cldr-staging/charts/37/supplemental/language_plural_rules.html
const POSSIBLE_MANY = [6, 7, 8, 11, 20, 21, 1000000, 0.5, 0.1, 0.0];
const POSSIBLE_FEW = [0, 2, 3, 4, 6];
const POSSIBLE_OTHER = [10, 11, 20, 100, 0.0, 0, 0.1, 2, 3, 4];

const SORTED_PLURALS: Intl.LDMLPluralRule[] = [
  'zero',
  'one',
  'two',
  'few',
  'many',
  'other',
];

export const getPluralVariants = (locale: string) => {
  const categories = new Set(
    new Intl.PluralRules(locale).resolvedOptions().pluralCategories
  );

  return SORTED_PLURALS.filter((i) => categories.has(i));
};

const findVariantExample = (
  variant: string,
  locale: string,
  list: number[]
) => {
  const intl = new Intl.PluralRules(locale);
  return list.find((num) => intl.select(num) === variant);
};

export const getVariantExample = (
  locale: string,
  variant: Intl.LDMLPluralRule
) => {
  switch (variant) {
    case 'zero':
      return findVariantExample('zero', locale, [0]);
    case 'one':
      return findVariantExample('one', locale, [1]);
    case 'two':
      return findVariantExample('two', locale, [2]);
    case 'few':
      return findVariantExample('few', locale, POSSIBLE_FEW);
    case 'many':
      return findVariantExample('many', locale, POSSIBLE_MANY);
    case 'other':
      return findVariantExample('other', locale, POSSIBLE_OTHER);
  }
};
