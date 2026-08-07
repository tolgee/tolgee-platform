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
  hoverTooltip,
  WidgetType,
} from '@codemirror/view';

import {
  findInvisibleCharacters,
  InvisibleChar,
} from 'tg.fixtures/invisibleCharacters';

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

export const invisibleCharactersTooltip = (
  getLabel: (char: InvisibleChar) => string
): Extension =>
  hoverTooltip((context, pos, side) => {
    const found = findInvisibleCharacters(context.state.doc.toString()).find(
      ({ index, char }) =>
        side < 0
          ? pos > index && pos <= index + char.value.length
          : pos >= index && pos < index + char.value.length
    );

    if (!found) {
      return null;
    }

    return {
      pos: found.index,
      end: found.index + found.char.value.length,
      above: true,
      create: () => {
        const dom = document.createElement('div');
        // Attributes must come from an object literal: `npm run generate-data-cy`
        // scans for `'data-cy': '<literal>'` and misses `setAttribute` with a variable.
        Object.entries({ 'data-cy': 'invisible-character-tooltip' }).forEach(
          ([name, value]) => dom.setAttribute(name, value)
        );
        dom.textContent = getLabel(found.char);
        return { dom };
      },
    };
  });
