import { FormatIcu } from '@tolgee/format-icu';
import { DevTools, Tolgee } from '@tolgee/web';

const apiKey = process.env.NEXT_PUBLIC_TOLGEE_API_KEY;
const apiUrl = process.env.NEXT_PUBLIC_TOLGEE_API_URL;

export const ALL_LOCALES = ['en', 'fr-FR', 'hi', 'ja-JP'];

export const DEFAULT_LOCALE = 'en';

export async function getStaticData(
  languages: string[],
  namespaces: string[] = ['']
) {
  const result: Record<string, any> = {};
  for (const lang of languages) {
    for (const namespace of namespaces) {
      if (namespace) {
        result[`${lang}:${namespace}`] = (
          await import(`../../messages/${namespace}/${lang}.json`)
        ).default;
      } else {
        result[lang] = (await import(`../../messages/${lang}.json`)).default;
      }
    }
  }
  return result;
}

export function TolgeeBase() {
  return Tolgee().use(FormatIcu()).use(DevTools()).updateDefaults({
    apiKey,
    apiUrl,
    fallbackLanguage: 'en',
  });
}
