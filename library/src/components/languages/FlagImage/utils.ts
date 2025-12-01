import { getSvgNameByEmoji } from '@tginternal/language-util';

// TODO Remove `removeBase` once `base` option gets available in webapp's Vite (works in library)
const removeBase = (basedFlagPaths: object, base: string) =>
  Object.fromEntries(
    Object.entries(basedFlagPaths).map(([key, value]) => [
      `./${key.slice(base.length)}`,
      value,
    ]),
  );

const flagPaths = removeBase(
  import.meta.glob<true, string, string>(
    '/node_modules/@tginternal/language-util/flags/*.svg',
    {
      query: '?url',
      import: 'default',
      // base: '/node_modules/@tginternal/language-util/',
      eager: true,
    },
  ),
  '/node_modules/@tginternal/language-util/',
);
export const flagNames = Object.keys(flagPaths).map((path) =>
  path.replace(/^\.\/flags\/(.*)\.svg$/, '$1'),
);
export const getFlagPath = (hex: string): string => {
  let flagName: string;
  try {
    flagName = getSvgNameByEmoji(hex);
  } catch {
    flagName = getSvgNameByEmoji('🏳️');
  }
  return flagPaths[`./flags/${flagName}.svg`];
};
