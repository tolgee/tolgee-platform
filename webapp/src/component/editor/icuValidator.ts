import * as monaco from 'monaco-editor';
import { parse } from '@formatjs/icu-messageformat-parser';

const icuValidator =
  (editor: typeof monaco.editor) => (model: monaco.editor.ITextModel) => {
    function validate() {
      const textToValidate = model.getValue();

      const markers: monaco.editor.IMarkerData[] = [];

      try {
        parse(textToValidate, { captureLocation: true });
      } catch (e) {
        const location = e.location;

        markers.push({
          severity: monaco.MarkerSeverity.Error,
          startLineNumber: location.start.line,
          startColumn: location.start.column,
          endLineNumber: location.end.line,
          endColumn: location.end.column,
          message: e.message,
        });
      }

      // change mySpecialLanguage to whatever your language id is
      editor.setModelMarkers(model, 'icu', markers);
    }

    model.onDidChangeContent(() => {
      validate();
    });
    validate();
  };

export default icuValidator;
