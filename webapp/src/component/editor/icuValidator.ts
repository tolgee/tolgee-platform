import { Ace, Range } from 'ace-builds';
import { parse } from '@formatjs/icu-messageformat-parser';

const icuValidator = (
  editor: Ace.Editor,
  onError?: (err: string | undefined) => void
) => {
  let lastMarker: number | undefined;
  function validate() {
    const textToValidate = editor.getSession().getValue();

    if (lastMarker !== undefined) {
      onError?.(undefined);
      editor.getSession().removeMarker(lastMarker);
      lastMarker = undefined;
    }

    try {
      parse(textToValidate, { captureLocation: true });
    } catch (e) {
      onError?.(e.message);

      const location = e.location;
      if (!location) {
        return;
      }

      const range = new Range(
        location.start.line - 1,
        location.start.column - 1,
        location.end.line - 1,
        location.end.column
      );

      const isShort =
        range.end.row === range.start.row &&
        range.end.column - range.start.column == 1;

      lastMarker = editor
        .getSession()
        .addMarker(range, isShort ? `error-highlight` : 'error', 'text', true);
    }
  }

  editor.on('change', () => {
    validate();
  });
  validate();
};

export default icuValidator;
