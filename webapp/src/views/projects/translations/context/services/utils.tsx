import type ReactList from 'react-list';
import {
  getPluralVariants,
  getTolgeeFormat,
  tolgeeFormatGenerateIcu,
} from '@tginternal/editor';
import {
  DeletableKeyWithTranslationsModelType,
  Edit,
  EditorProps,
} from '../types';

export function generateCurrentValue(
  position: EditorProps,
  textValue: string | undefined,
  key: DeletableKeyWithTranslationsModelType | undefined,
  raw: boolean
): Edit {
  const result: Edit = {
    ...position,
    activeVariant: position.activeVariant ?? 'other',
    value: { variants: { other: textValue } },
  };
  if (position.language && key?.keyIsPlural) {
    const format = getTolgeeFormat(textValue ?? '', key.keyIsPlural, raw);
    const variants = getPluralVariants(position.language);
    if (!position.activeVariant) {
      result.activeVariant = variants[0];
    }
    result.value = format;
    result.value.parameter = key.keyPluralArgName ?? 'value';
  }
  return result;
}

export function composeValue(position: Edit, raw: boolean) {
  if (position.value) {
    return tolgeeFormatGenerateIcu(position.value, raw);
  }
  return position.value;
}

export function serializeVariants(
  variants: Record<string, string | undefined> | undefined
) {
  if (!variants) {
    return '';
  }
  return Object.entries(variants)
    .sort(([keyA], [keyB]) => keyA.localeCompare(keyB))
    .map(([_, value]) => value)
    .filter((value) => Boolean(value))
    .join('<%>');
}

/**
 * Kinda hacky way how to update react-list size cache, when editor gets open
 */
export function updateReactListSizes(list: ReactList, currentIndex: number) {
  // @ts-ignore
  const cache = list.cache as Record<number, number>;
  // @ts-ignore
  const from = list.state.from as number;
  // @ts-ignore
  const itemEls = list.items.children;
  const elementIndex = currentIndex - from;
  const previousSize = cache[currentIndex];
  const currentSize = itemEls[elementIndex]?.['offsetHeight'];
  // console.log({ previousSize, currentSize });
  if (currentSize !== previousSize && typeof currentSize === 'number') {
    cache[currentIndex] = currentSize;
    // @ts-ignore
    list.updateFrameAndClearCache();
    list.setState((state) => ({ ...state }));
  }
}
