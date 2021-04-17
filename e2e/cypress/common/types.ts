export type Scope = "translations.edit" | "translations.view" | "keys.edit";
export type ArgumentTypes<F extends Function> = F extends (...args: infer A) => any ? A : never;
