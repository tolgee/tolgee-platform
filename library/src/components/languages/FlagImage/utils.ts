import { getSvgNameByEmoji } from '@tginternal/language-util';

const removePaths = (
  filesByPaths: Record<string, string>,
): Record<string, string> =>
  Object.fromEntries(
    Object.entries(filesByPaths).map(([key, value]) => [
      key.replace(/^.*\/flags\/(.*)\.svg$/, '$1'),
      value,
    ]),
  );

const flagFiles = removePaths(
  import.meta.glob<true, string, string>(
    '/node_modules/@tginternal/language-util/flags/*.svg',
    { query: '?url', import: 'default', eager: true },
  ),
);

export const flagNames = Object.keys(flagFiles);

export const getFlagPath = (hex: string): string => {
  let flagName: string;
  try {
    flagName = getSvgNameByEmoji(hex);
  } catch {
    flagName = getSvgNameByEmoji('🏳️');
  }
  return flagFiles[flagName];
};
