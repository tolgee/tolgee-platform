export type LiteralUnion<LiteralType extends BaseType, BaseType> =
  | LiteralType
  | (BaseType & { _?: never });
