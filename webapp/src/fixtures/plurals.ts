import { TolgeeFormat } from '@tginternal/editor';

export function isEmpty(value: TolgeeFormat | undefined) {
  if (!value) {
    return true;
  }
  if (!value.variants) {
    return true;
  }
  if (value.parameter) {
    return !Object.values(value.variants).some(Boolean);
  } else {
    return !value.variants.other;
  }
}

export function isUnchanged(
  value: TolgeeFormat | undefined,
  translation: string | undefined
) {
  if (!value?.parameter) {
    return value?.variants.other === translation;
  }
  return false;
}
