import { useTranslate } from '@tolgee/react';

type TFunction = ReturnType<typeof useTranslate>['t'];

export type HandlebarsHelper = {
  name: string;
  detail: string;
  description: string;
};

export const getHandlebarsHelpers = (t: TFunction): HandlebarsHelper[] => [
  {
    name: 'escapeJson',
    detail: t('ai_prompt_helper_escape_json_detail', 'Escape for JSON'),
    description: t(
      'ai_prompt_helper_escape_json_description',
      'Escapes the value (quotes, newlines, backslashes), so it can be safely embedded inside a JSON string or a double-quoted YAML scalar.'
    ),
  },
];
