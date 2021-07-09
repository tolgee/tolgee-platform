import { languages } from 'monaco-editor';
const tokenizer: languages.IMonarchLanguage = {
  defaultToken: 'invalid',
  brackets: [{ open: '{', close: '}', token: 'brackets.curly' }],
  tokenizer: {
    root: [
      [/'([^']+)'/, 'string'],
      [/'./, 'string'],
      [/{/, 'brackets.curly', '@argument'],
      [/#/, 'keyword'],
      [/[^{]/, 'string'],
    ],
    text: [[/}/, 'brackets.curly', '@pop'], { include: 'root' }],
    argument: [
      [
        /([\s\n\r]*)([\w]+)([\s\n\r]*)(,)/,
        [
          { token: '' },
          { token: 'variable' },
          { token: '' },
          { token: 'expression.delimiter', switchTo: '@expression' },
        ],
      ],
      [
        /([\s\n\r]*)([\w]+)([\s\n\r]*)(})/,
        [
          { token: '' },
          { token: 'variable' },
          { token: '' },
          { token: 'bracket.curl', next: '@pop' },
        ],
      ],
    ],
    expression: [
      [/\s+/, ''],
      [/,/, 'expression.delimiter'],
      [/{/, 'brackets.curly', '@text'],
      [/}/, 'brackets.curly', '@pop'],
      [/[^\s]/, 'keyword'],
    ],
  },
};

export default tokenizer;
