import {
  EditorState,
  Extension,
  RangeSetBuilder,
  StateField,
} from '@codemirror/state';
import {
  Decoration,
  DecorationSet,
  EditorView,
  WidgetType,
} from '@codemirror/view';

import { findInvisibleCharacters } from 'tg.fixtures/invisibleCharacters';

const nonBreakingSpaceDecoration = Decoration.mark({
  attributes: { class: 'cm-invisible-char-nbsp' },
});

class ZeroWidthWidget extends WidgetType {
  eq() {
    return true;
  }

  toDOM() {
    const bar = document.createElement('span');
    bar.className = 'cm-invisible-char-zero-width';
    return bar;
  }
}

const zeroWidthDecoration = Decoration.widget({
  widget: new ZeroWidthWidget(),
  side: 1,
});

function buildDecorations(state: EditorState) {
  const builder = new RangeSetBuilder<Decoration>();
  findInvisibleCharacters(state.doc.toString()).forEach(({ index, char }) => {
    if (char.kind === 'zeroWidth') {
      builder.add(index, index, zeroWidthDecoration);
    } else {
      builder.add(index, index + char.value.length, nonBreakingSpaceDecoration);
    }
  });
  return builder.finish();
}

const invisibleCharactersField = StateField.define<DecorationSet>({
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

export const invisibleCharactersPlugin = (): Extension[] => [
  invisibleCharactersField,
];
