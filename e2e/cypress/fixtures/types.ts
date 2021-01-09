export type Scope = "translations.edit" | "translations.view" | "keys.edit";
export type ArgumentTypes<F extends Function> = F extends (...args: infer A) => any ? A : never;
type Tail<T extends any[]> = ((...args: T) => any) extends ((
    _: infer First,
    ...rest: infer Rest
    ) => any)
    ? T extends any[] ? Rest : ReadonlyArray<Rest[number]>
    : []