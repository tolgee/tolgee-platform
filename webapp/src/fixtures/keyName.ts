import { PO_MSGCTXT_KEY_SEPARATOR } from '@tginternal/editor';

export { PO_MSGCTXT_KEY_SEPARATOR };

export type SplitKeyName = {
  msgctxt?: string;
  msgid: string;
};

export function splitKeyName(name: string): SplitKeyName {
  const i = name.indexOf(PO_MSGCTXT_KEY_SEPARATOR);
  if (i < 0) return { msgid: name };
  return { msgctxt: name.slice(0, i), msgid: name.slice(i + 1) };
}

export const KEY_NAME_LEADING_WHITESPACE_REGEX = /^(\s+)/;
export const KEY_NAME_TRAILING_WHITESPACE_REGEX = /(\s+)$/;

export function hasOuterWhitespace(name: string): boolean {
  return (
    KEY_NAME_LEADING_WHITESPACE_REGEX.test(name) ||
    KEY_NAME_TRAILING_WHITESPACE_REGEX.test(name)
  );
}
