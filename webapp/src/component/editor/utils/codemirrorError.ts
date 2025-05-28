import {
  EditorView,
  Decoration,
  DecorationSet,
  hoverTooltip,
} from '@codemirror/view';
import { StateField, StateEffect, Range } from '@codemirror/state';

// Define the structure of an error
export type EditorError = {
  line: number; // 1-based line number
  column: number; // 1-based column number
  message: string; // Error message to display on hover
};

// Effect to set errors
export const setErrorsEffect = StateEffect.define<EditorError[]>();

// State field to store the error data
const errorDataField = StateField.define<EditorError[]>({
  create() {
    return [];
  },
  update(errors, tr) {
    tr.state;
    for (const effect of tr.effects) {
      if (effect.is(setErrorsEffect)) {
        return effect.value;
      }
    }
    return errors;
  },
});

// State field to manage error decorations
const errorField = StateField.define<DecorationSet>({
  create() {
    return Decoration.none;
  },
  update(decorations, tr) {
    // Map existing decorations to new positions after document changes
    decorations = decorations.map(tr.changes);

    // Handle error updates from the effect
    for (const effect of tr.effects) {
      if (effect.is(setErrorsEffect)) {
        decorations = Decoration.none; // Clear previous decorations
        const newDecorations: Range<Decoration>[] = [];

        for (const error of effect.value) {
          const lineNum = error.line - 1; // Convert to 0-based
          const colNum = error.column - 1; // Convert to 0-based

          // Get the position in the document
          const line = tr.state.doc.line(lineNum + 1); // Line is 1-based in doc
          const from = line.from + colNum;
          const to = from + 1; // Underline a single character for simplicity

          if (from < tr.state.doc.length) {
            newDecorations.push(errorDecoration.range(from, to));
          }
        }

        decorations = Decoration.set(newDecorations);
      }
    }
    return decorations;
  },
  provide: (f) => EditorView.decorations.from(f),
});

// Decoration for the error underline
const errorDecoration = Decoration.mark({
  class: 'cm-error-underline',
});

// Hover tooltip provider
const errorTooltip = hoverTooltip((view, pos, side) => {
  const errors = view.state.field(errorDataField);
  const doc = view.state.doc;
  const error = errors.find((e) => {
    const line = doc.line(e.line);
    const from = line.from + e.column - 1;
    return pos >= from && pos <= from + 1;
  });

  if (!error) return null;

  return {
    pos,
    end: pos + 1,
    create: () => {
      const dom = document.createElement('div');
      dom.textContent = error.message;
      dom.className = 'cm-error-tooltip';
      return { dom };
    },
    above: true,
  };
});
export function errorPlugin() {
  return [errorDataField, errorField, errorTooltip];
}
