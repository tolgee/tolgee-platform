export type LiteralUnion<LiteralType extends BaseType, BaseType> =
  | LiteralType
  | (BaseType & { _?: never });

// Utility to check the array is exhaustive
export type CheckExhaustiveness<
  T extends readonly any[],
  U
> = T[number] extends U
  ? Exclude<U, T[number]> extends never
    ? true
    : false
  : false;
