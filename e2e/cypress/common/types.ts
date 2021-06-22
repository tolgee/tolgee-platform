export type Scope = 'translations.edit' | 'translations.view' | 'keys.edit';
// eslint-disable-next-line @typescript-eslint/ban-types
export type ArgumentTypes<F extends Function> = F extends (
  ...args: infer A
) => any
  ? A
  : never;
