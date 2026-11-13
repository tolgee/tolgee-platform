import { useTranslate } from '@tolgee/react';

type TranslateFn = ReturnType<typeof useTranslate>['t'];

export type ModuleErrorTranslator = (
  t: TranslateFn,
  code: string,
  params?: string[]
) => string | undefined;

export const moduleErrorTranslator: { translate: ModuleErrorTranslator } = {
  translate: () => undefined,
};
