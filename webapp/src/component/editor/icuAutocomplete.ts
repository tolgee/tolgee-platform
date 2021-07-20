import { Ace } from 'ace-builds';
import 'ace-builds/src-noconflict/ext-language_tools';

const FUNCTION_NAMES = [
  'plural',
  'select',
  'selectordinal',
  'number',
  'date',
  'time',
];

export const icuAutocompete = {
  getCompletions: function (
    _editor: Ace.Editor,
    session: Ace.EditSession,
    pos,
    _prefix,
    callback
  ) {
    const token = session.getTokenAt(pos.row, pos.column);
    let suggestions: any[] | null = null;

    if (token?.type === 'expression.function') {
      suggestions = FUNCTION_NAMES.map(function (word) {
        return {
          caption: word,
          value: word,
        };
      });
    }
    callback(null, suggestions);
  },
};
