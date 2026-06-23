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

export function hasOuterWhitespace(name: string): boolean {
  return name !== name.trim();
}
