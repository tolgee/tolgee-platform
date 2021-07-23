import { useState } from 'react';
import CodeMirror from 'codemirror';
import { Controlled as CodeMirrorReact } from 'react-codemirror2';
import icuMode from './icuMode';
import { parse } from '@formatjs/icu-messageformat-parser';
import 'codemirror/keymap/sublime';
import 'codemirror/lib/codemirror.css';
import 'codemirror/addon/lint/lint';
import 'codemirror/addon/lint/lint.css';

import { makeStyles } from '@material-ui/core';
import { useMemo } from 'react';
import { useEffect } from 'react';

type Props = {
  initialValue: string;
  onChange?: (val: string) => void;
  onSave?: (val: string) => void;
  onCancel?: () => void;
  background?: string;
  plaintext?: boolean;
};

const useStyles = makeStyles((theme) => ({
  '@global': {
    '.CodeMirror-lint-tooltip': {
      background: 'white',
      borderRadius: 0,
    },
    '.CodeMirror-lint-message-error': {
      backgroundImage: 'unset',
      paddingLeft: 0,
    },
    '.CodeMirror-gutters': {
      border: 0,
      background: 'transparent',
    },
  },
  editor: {
    '& .CodeMirror': {
      minHeight: 100,
      height: '100%',
      // @ts-ignore
      background: (props) => props.background,
    },
    '& .CodeMirror-line': {
      fontFamily:
        '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol" !important',
    },
    '& .cm-function': {
      color: '#007300',
    },
    '& .cm-parameter': {
      color: '#002bff',
    },
    '& .cm-option': {
      color: '#002bff',
    },
    '& .cm-keyword': {
      color: '#002bff',
    },
    '& .cm-string': {
      color: '#000000',
    },
    '& .cm-bracket': {
      color: '#002bff',
    },
  },
}));

function linter(text: string, data: any) {
  const { errors } = data;
  return errors?.map((error) => {
    const { location = { start: { line: 1 } } } = error;
    const {
      start,
      end = { ...start, column: start.column ? start.column + 1 : 1 },
    } = location;
    const startColumn = start.column ? start.column - 1 : 0;
    const endColumn = end.column ? end.column - 1 : 0;
    const hint = {
      message: error.message,
      severity: 'error',
      type: 'validation',
      from: CodeMirror.Pos(start.line - 1, startColumn - 1),
      to: CodeMirror.Pos(end.line - 1, endColumn - 1),
    };
    return hint;
  });
}

CodeMirror.registerHelper('lint', 'icu', linter);

export const Editor: React.FC<Props> = ({
  initialValue,
  onChange,
  onCancel,
  onSave,
  plaintext,
  background,
}) => {
  const classes = useStyles({ background });

  const [value, setValue] = useState(initialValue);

  useEffect(() => {
    setValue(initialValue);
  }, [initialValue]);

  const handleChange = (val: string) => {
    onChange?.(val);
    setValue(val);
  };

  const error = useMemo(() => {
    try {
      parse(value, { captureLocation: true });
    } catch (e) {
      return e;
    }
  }, [value]);

  const options: CodeMirror.EditorConfiguration = {
    lineNumbers: false,
    mode: plaintext ? undefined : 'icu',
    autofocus: true,
    lineWrapping: true,
    keyMap: 'sublime',
    extraKeys: {
      Enter: (editor) => onSave?.(editor.getValue()),
      Esc: () => onCancel?.(),
    },
    gutters: ['CodeMirror-lint-markers'],
    lint: {
      // @ts-ignore
      errors: error ? [error] : [],
    },
  };

  return (
    <CodeMirrorReact
      className={classes.editor}
      value={value}
      // @ts-ignore
      defineMode={{ name: 'icu', fn: icuMode }}
      options={options}
      onBeforeChange={(editor, data, value) => {
        handleChange(value);
      }}
    />
  );
};
