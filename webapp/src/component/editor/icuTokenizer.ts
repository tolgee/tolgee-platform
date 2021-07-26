import 'ace-builds/src-noconflict/ace';
import 'ace-builds/src-noconflict/mode-text';
const Tokenizer = ace.require('ace/tokenizer').Tokenizer;

export const rules = {
  start: [
    { regex: /'([^']+)'/, token: 'string' },
    { regex: /'./, token: 'string' },
    { regex: /{/, token: 'expression.bracket', next: 'firstParam' },
    { regex: /}/, token: 'expression.bracket.text', next: 'expression' },
    { regex: /#/, token: 'expression.keyword' },
    { regex: /[^{]/, token: 'string' },
  ],
  firstParam: [
    { regex: /\s+/, token: 'expression.space' },
    { regex: /,/, token: 'expression.delimiter', next: 'secondParam' },
    { regex: /}/, token: 'expression.bracket', next: 'start' },
    { regex: /[^\s]/, token: 'expression.parameter' },
  ],
  secondParam: [
    { regex: /\s+/, token: 'expression.space' },
    { regex: /,/, token: 'expression.delimiter', next: 'expression' },
    { regex: /}/, token: 'expression.bracket', next: 'start' },
    { regex: /[^\s]/, token: 'expression.function' },
  ],
  expression: [
    { regex: /\s+/, token: 'expression.space' },
    { regex: /,/, token: 'expression.delimiter' },
    { regex: /{/, token: 'expression.bracket.text', next: 'start' },
    { regex: /}/, token: 'expression.bracket', next: 'start' },
    { regex: /[^\s]/, token: 'expression.option' },
  ],
};

export type TokenType = {
  type: string;
  value: string;
};

const tokenizer = new Tokenizer(rules);
export const tokenize = (text: string): TokenType[] => {
  return tokenizer.getLineTokens(text).tokens;
};
