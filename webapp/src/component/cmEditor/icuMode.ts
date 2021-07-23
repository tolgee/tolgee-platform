import { ModeFactory, StringStream } from 'codemirror';

type Frame =
  | {
      type: 'argument';
      indentation: number;
      formatType?: string;
      argPos: number;
    }
  | {
      type: 'escaped';
    }
  | {
      type: 'text';
    };

interface ModeState {
  stack: Frame[];
}

const mode: ModeFactory<ModeState> = (
  { indentUnit = 2 } = {},
  { apostropheMode = 'DOUBLE_OPTIONAL' } = {}
) => {
  function peek(stream: StringStream, offset = 0) {
    return stream.string.charAt(stream.pos + offset) || undefined;
  }

  function eatEscapedStringStart(stream: StringStream, inPlural: boolean) {
    const nextChar = stream.peek();
    if (nextChar === "'") {
      if (apostropheMode === 'DOUBLE_OPTIONAL') {
        const nextAfterNextChar = peek(stream, 1);
        if (
          nextAfterNextChar === "'" ||
          nextAfterNextChar === '{' ||
          (inPlural && nextAfterNextChar === '#')
        ) {
          stream.next();
          return true;
        }
      } else {
        stream.next();
        return true;
      }
    }
    return false;
  }

  function eatEscapedStringEnd(stream: StringStream) {
    const nextChar = peek(stream, 0);
    if (nextChar === "'") {
      const nextAfterNextChar = peek(stream, 1);
      if (!nextAfterNextChar || nextAfterNextChar !== "'") {
        stream.next();
        return true;
      }
    }
    return false;
  }

  function pop(stack: Frame[]) {
    if (stack.length > 1) {
      stack.pop();
      return true;
    }
    return false;
  }

  return {
    startState() {
      return {
        stack: [
          {
            type: 'text',
          },
        ],
      };
    },

    copyState(state) {
      return {
        stack: state.stack.map((frame) => Object.assign({}, frame)),
      };
    },

    token(stream, state) {
      const current = state.stack[state.stack.length - 1];
      const isInsidePlural = !!state.stack.find(
        (frame) =>
          frame.type === 'argument' &&
          frame.formatType &&
          ['selectordinal', 'plural'].includes(frame.formatType)
      );

      if (current.type === 'escaped') {
        if (eatEscapedStringEnd(stream)) {
          pop(state.stack);
          return 'string-2';
        }

        stream.match("''") || stream.next();
        return 'string-2';
      }

      if (current.type === 'text') {
        if (eatEscapedStringStart(stream, isInsidePlural)) {
          state.stack.push({ type: 'escaped' });
          return 'string-2';
        }

        if (isInsidePlural && stream.eat('#')) {
          return 'keyword';
        }

        if (stream.eat('{')) {
          state.stack.push({
            type: 'argument',
            indentation: stream.indentation() + indentUnit,
            argPos: 0,
          });
          return 'bracket';
        }

        if (stream.peek() === '}') {
          if (pop(state.stack)) {
            stream.next();
            return 'bracket';
          }
        }

        stream.next();
        return 'string';
      }

      if (current.type === 'argument') {
        const inId = current.argPos === 0;
        const inFn = current.argPos === 1;
        const inFormat = current.argPos === 2;
        if (stream.match(/\s*,\s*/)) {
          current.argPos += 1;
          return null;
        }
        if (inId && stream.eatWhile(/[a-zA-Z0-9_]/)) {
          return 'def';
        }
        if (
          inFn &&
          stream.match(/(selectordinal|plural|select|number|date|time)\b/)
        ) {
          current.formatType = stream.current();
          return 'function';
        }
        if (inFormat && stream.match(/offset\b/)) {
          return 'option';
        }
        if (inFormat && stream.eat('=')) {
          return 'operator';
        }
        if (
          inFormat &&
          current.formatType &&
          ['selectordinal', 'plural'].includes(current.formatType) &&
          stream.match(/zero|one|two|few|many/)
        ) {
          return 'option';
        }
        if (inFormat && stream.match('other')) {
          return 'option';
        }
        if (inFormat && stream.match(/[0-9]+\b/)) {
          return 'number';
        }
        if (inFormat && stream.eatWhile(/[a-zA-Z0-9_]/)) {
          return 'variable';
        }
        if (inFormat && stream.eat('{')) {
          state.stack.push({ type: 'text' });
          return 'bracket';
        }
        if (stream.eat('}')) {
          pop(state.stack);
          return 'bracket';
        }
      }

      if (!stream.eatSpace()) {
        stream.next();
      }

      return null;
    },

    blankLine(state) {
      const current = state.stack[state.stack.length - 1];
      if (current.type === 'text') {
        return 'cm-string';
      }
      return undefined;
    },

    indent(state, textAfter) {
      const current = state.stack[state.stack.length - 1];
      if (!current || current.type === 'text' || current.type === 'escaped') {
        return 0;
      }
      if (textAfter[0] === '}') {
        return current.indentation - indentUnit;
      }
      return current.indentation;
    },
  };
};

export default mode;
