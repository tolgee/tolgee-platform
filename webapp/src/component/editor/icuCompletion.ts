import { languages } from 'monaco-editor';

type CompletionOptions = {
  variables: string[];
  enableSnippets: boolean;
};

function icuCompletion({
  variables,
  enableSnippets,
}: CompletionOptions): languages.CompletionItemProvider {
  return {
    provideCompletionItems: (model, position) => {
      const textUntilPosition = model.getValueInRange({
        startLineNumber: 1,
        startColumn: 1,
        endLineNumber: position.lineNumber,
        endColumn: position.column,
      });

      const word = model.getWordUntilPosition(position);
      const range = {
        startLineNumber: position.lineNumber,
        endLineNumber: position.lineNumber,
        startColumn: word.startColumn,
        endColumn: word.endColumn,
      };

      let suggestions: languages.CompletionItem[] = [];

      if (textUntilPosition.match(/{[\s\n\r]*[^,{}]+$/)) {
        // suggest variables
        suggestions = variables.map((v) => ({
          label: v,
          kind: languages.CompletionItemKind.Variable,
          insertText: v,
          range,
        }));
      } else if (textUntilPosition.match(/{[\s\n\r]*[^,{}]+,[^,{}]+$/)) {
        // we are on function
        const lastLine = model.getLineCount();
        const textAfterPosition = model
          .getValueInRange({
            startLineNumber: position.lineNumber,
            startColumn: position.column,
            endLineNumber: lastLine,
            endColumn: model.getLineLength(lastLine),
          })
          .substring(1);

        suggestions.push(
          {
            label: 'number',
            kind: languages.CompletionItemKind.Function,
            insertText: 'number',
            range,
          },
          {
            label: 'date',
            kind: languages.CompletionItemKind.Function,
            insertText: 'date',
            range,
          },
          {
            label: 'time',
            kind: languages.CompletionItemKind.Function,
            insertText: 'time',
            range,
          }
        );

        if (enableSnippets && textAfterPosition.match(/^[\s\n\r]*(}|$)/)) {
          // suggest snippets
          suggestions.push(
            {
              label: 'plural',
              kind: languages.CompletionItemKind.Function,
              insertText: 'plural,\n\t$1 {$2}\n',
              insertTextRules:
                languages.CompletionItemInsertTextRule.InsertAsSnippet,
              range,
            },
            {
              label: 'select',
              kind: languages.CompletionItemKind.Function,
              insertText: 'select,\n\t$1 {$2}\n',
              insertTextRules:
                languages.CompletionItemInsertTextRule.InsertAsSnippet,
              range,
            },
            {
              label: 'selectordinal',
              kind: languages.CompletionItemKind.Function,
              insertText: 'selectordinal,\n\t$1 {$2}\n',
              insertTextRules:
                languages.CompletionItemInsertTextRule.InsertAsSnippet,
              range,
            }
          );
        } else {
          // suggest function names
          suggestions.push(
            {
              label: 'plural',
              kind: languages.CompletionItemKind.Function,
              insertText: 'plural',
              range,
            },
            {
              label: 'selectordinal',
              kind: languages.CompletionItemKind.Function,
              insertText: 'selectordinal',
              range,
            },
            {
              label: 'select',
              kind: languages.CompletionItemKind.Function,
              insertText: 'select',
              range,
            }
          );
        }
      }
      return {
        suggestions: suggestions,
      } as languages.ProviderResult<languages.CompletionList>;
    },
  };
}

export default icuCompletion;
