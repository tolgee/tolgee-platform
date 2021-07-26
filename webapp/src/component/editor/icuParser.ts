import { tokenize } from './icuTokenizer';

export type ParameterType = {
  name: string;
  function: string | null;
  options: string[];
};

export const getParameters = (text: string) => {
  const final: ParameterType[] = [];
  const stack: ParameterType[] = [];

  const tokens = tokenize(text);

  for (const token of tokens) {
    const lastOnStack = stack[stack.length - 1];
    if (token.type === 'expression.bracket') {
      if (token.value === '}') {
        const last = stack.pop();
        if (last) {
          final.push(last);
        }
      }
    } else if (token.type === 'expression.parameter') {
      stack.push({
        name: token.value,
        function: null,
        options: [],
      });
    } else if (token.type === 'expression.function') {
      if (lastOnStack) {
        lastOnStack.function = token.value;
      }
    } else if (token.type === 'expression.option') {
      lastOnStack?.options.push(token.value);
    }
  }
  return final;
};
