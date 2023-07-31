import type en from './src/i18n/en.json';

declare module '@tolgee/core/lib/types' {
  type TranslationsType = typeof en;

  // this will make sure that nested keys are accessible with "."
  // if you use only flat keys, it's not necessary
  type DotNotationEntries<T> = T extends object
    ? {
        [K in keyof T]: `${K & string}${T[K] extends undefined
          ? ''
          : T[K] extends object
          ? `.${DotNotationEntries<T[K]>}`
          : ''}`;
      }[keyof T]
    : '';

  // this enables both intellisense and new keys without an error
  type LiteralUnion<LiteralType extends BaseType, BaseType extends Primitive> =
    | LiteralType
    | (BaseType & { _?: never });

  export type TranslationKey = LiteralUnion<
    DotNotationEntries<TranslationsType>,
    string
  >;
}
