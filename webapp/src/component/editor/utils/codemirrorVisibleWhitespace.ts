import {
  EditorState,
  Extension,
  RangeSetBuilder,
  StateField,
} from '@codemirror/state';
import { Decoration, DecorationSet, EditorView } from '@codemirror/view';

import {
  KEY_NAME_LEADING_WHITESPACE_REGEX,
  KEY_NAME_TRAILING_WHITESPACE_REGEX,
} from 'tg.fixtures/keyName';

// Marks the css class for space
const spaceDecoration = Decoration.mark({
  attributes: { class: 'cm-keyname-space-indicator' },
});

//Decorates the leading and trailing whitespace
function buildDecorations(state: EditorState) {
  const builder = new RangeSetBuilder<Decoration>();
  const text = state.doc.toString();

  const leading = text.match(KEY_NAME_LEADING_WHITESPACE_REGEX);
  const leadingLength = leading?.[1].length ?? 0;
  for (let i = 0; i < leadingLength; i += 1) {
    builder.add(i, i + 1, spaceDecoration);
  }

  const trailing = text.match(KEY_NAME_TRAILING_WHITESPACE_REGEX);
  const trailingLength = trailing?.[1].length ?? 0;
  const trailingStart = Math.max(text.length - trailingLength, leadingLength);
  for (let i = trailingStart; i < text.length; i += 1) {
    builder.add(i, i + 1, spaceDecoration);
  }

  return builder.finish();
}

//Marks the field with the decorator
const visibleSpaceField = StateField.define<DecorationSet>({
  create(state) {
    return buildDecorations(state);
  },
  update(decorations, tr) {
    if (!tr.docChanged) {
      return decorations;
    }
    return buildDecorations(tr.state);
  },
  provide: (field) => EditorView.decorations.from(field),
});

export const visibleKeyNameSpacesPlugin = (): Extension[] => [
  visibleSpaceField,
];
