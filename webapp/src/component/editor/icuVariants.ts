import IntlMessageFormat from 'intl-messageformat';
import { getParameters, ParameterType } from './icuParser';

const ENUMERABLE_FUNCTIONS = ['plural', 'select', 'selectordinal'];

type VariantType = { option: string; value: string };

type VariantsSummaryType = {
  parameters: ParameterType[];
  variants: VariantType[] | null;
  parseError: string | null;
};

// Taken from
// https://unicode-org.github.io/cldr-staging/charts/37/supplemental/language_plural_rules.html
const POSSIBLE_MANY = [6, 7, 8, 11, 20, 21, 1000000, 0.5, 0.1, 0.0];
const POSSIBLE_FEW = [0, 2, 3, 4, 6];
const POSSIBLE_OTHER = [10, 11, 20, 100, 0.0, 0, 0.1, 2, 3, 4];

const getExampleValue = (paramName: string, func: string | null) => {
  switch (func) {
    case 'number':
      return 42;
    case 'date':
    case 'time':
      return new Date();
    case 'tag':
      return (content) => `<${paramName}>${content}</${paramName}>`;
    default:
      return `{${paramName}}`;
  }
};

const findCategoryExample = (
  category: string,
  locale: string,
  list: number[],
  type: Intl.PluralRuleType
) => {
  const intl = new Intl.PluralRules(locale, { type });
  return list.find((num) => intl.select(num) === category);
};

const getPluralExample = (
  option: string,
  locale: string,
  type: Intl.PluralRuleType
) => {
  if (option[0] === '=') {
    return Number(option.replace('=', ''));
  } else if (option === 'zero') {
    return findCategoryExample('zero', locale, [0], type);
  } else if (option === 'one') {
    return findCategoryExample('one', locale, [1], type);
  } else if (option === 'two') {
    return findCategoryExample('two', locale, [2], type);
  } else if (option === 'few') {
    return findCategoryExample('few', locale, POSSIBLE_FEW, type);
  } else if (option === 'many') {
    return findCategoryExample('many', locale, POSSIBLE_MANY, type);
  } else if (option === 'other') {
    return findCategoryExample('other', locale, POSSIBLE_OTHER, type);
  } else {
    return undefined;
  }
};

export const icuVariants = (
  text: string,
  locale = 'en'
): VariantsSummaryType => {
  let variants: VariantType[] | null = null;
  let parseError: string | null = null;
  const functions: ParameterType[] = [];
  const variables: ParameterType[] = [];

  let parameters: ParameterType[] = [];

  try {
    parameters = getParameters(text);

    for (const p of parameters) {
      if (ENUMERABLE_FUNCTIONS.includes(p.function!) && p.options?.length > 0) {
        functions.push(p);
      } else {
        variables.push(p);
      }
    }

    const staticVariables = variables.reduce(
      (obj, item) => ({
        ...obj,
        [item.name]: getExampleValue(item.name, item.function),
      }),
      {}
    );

    if (functions.length === 0 && parameters.length) {
      const formattedValue = new IntlMessageFormat(text, locale, undefined, {
        ignoreTag: true,
      }).format(staticVariables);
      variants = [
        {
          value: formattedValue as string,
          option: '',
        },
      ];
    } else if (functions.length === 1) {
      const func = functions[0];
      if (func.function === 'select') {
        variants = func.options
          .map((option) => {
            const formattedValue = new IntlMessageFormat(
              text,
              locale,
              undefined,
              { ignoreTag: true }
            ).format({
              ...staticVariables,
              [func.name]: option,
            });
            return {
              value: formattedValue as string,
              option,
            };
          })
          .filter(Boolean) as VariantType[];
      } else if (
        func.function === 'plural' ||
        func.function === 'selectordinal'
      ) {
        variants = func.options
          .map((option) => {
            const example = getPluralExample(
              option,
              locale,
              func.function === 'plural' ? 'cardinal' : 'ordinal'
            );
            if (example === undefined) {
              return {
                value: '-',
                option,
              };
            }
            const formattedValue = new IntlMessageFormat(
              text,
              locale,
              undefined,
              { ignoreTag: true }
            ).format({
              ...staticVariables,
              [func.name]: example,
            });
            return {
              value: formattedValue as string,
              option,
            };
          })
          .filter(Boolean) as VariantType[];
      }
    }
  } catch (e: any) {
    variants = null;
    parseError = e.message;
  }

  return {
    parameters,
    variants: variants?.length ? variants : null,
    parseError,
  };
};
