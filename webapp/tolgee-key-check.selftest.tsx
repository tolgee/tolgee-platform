import { T, TranslationKey, useTranslate } from '@tolgee/react';

// Canary for the strict missing-key check (augmentation in tolgee.prod.d.ts).
// Both assertions must stay — deleting them, or the bogus key, silently disables
// the missing-key CI guard. They fail if the augmentation breaks either way: the
// bogus key stops erroring (widened to `string`), or `TranslationKey` collapses
// to `never` (over-narrowed).
type Assert<Condition extends true> = Condition;
export type _TranslationKeyNotEmpty = Assert<
  [TranslationKey] extends [never] ? false : true
>;

export function tolgeeKeyCheckSelfTest() {
  const { t } = useTranslate();
  // @ts-expect-error
  t('__tolgee_key_check_must_stay_strict__');
  // @ts-expect-error
  return <T keyName="__tolgee_key_check_must_stay_strict__" />;
}
