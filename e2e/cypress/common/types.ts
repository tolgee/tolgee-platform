export type Scope =
  | 'translations.view'
  | 'translations.edit'
  | 'keys.edit'
  | 'screenshots.view'
  | 'screenshots.upload'
  | 'screenshots.delete';

// eslint-disable-next-line @typescript-eslint/ban-types
export type ArgumentTypes<F extends Function> = F extends (
  ...args: infer A
) => any
  ? A
  : never;
