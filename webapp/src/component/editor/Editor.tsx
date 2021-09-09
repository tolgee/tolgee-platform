import { useMemo } from 'react';
import CodeMirror from 'codemirror';
import { Controlled as CodeMirrorReact } from 'react-codemirror2';
import { parse } from '@formatjs/icu-messageformat-parser';
import 'codemirror/keymap/sublime';
import 'codemirror/lib/codemirror.css';
import 'codemirror/addon/lint/lint';
import 'codemirror/addon/lint/lint.css';
import { makeStyles } from '@material-ui/core';
import { useTranslate } from '@tolgee/react';

import icuMode from './icuMode';

export type Direction = 'DOWN';

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
    display: 'flex',
    flexGrow: 1,
    alignItems: 'stretch',
    '& .react-codemirror2': {
      display: 'flex',
      flexGrow: 1,
      position: 'relative',
    },
    '& *': {
      overflow: 'hidden !important',
    },
    '& .CodeMirror': {
      width: '100%',
      // @ts-ignore
      minHeight: (props) => props.minHeight,
      height: '100%',
      marginLeft: -5,
      // @ts-ignore
      background: (props) => props.background,
    },
    '& .CodeMirror-lines': {
      padding: '0px !important',
    },
    '& .CodeMirror-line': {
      padding: '0px !important',
      fontFamily:
        '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol" !important',
    },
    '& .CodeMirror-lint-markers': {
      width: 5,
    },
    '& .CodeMirror-lint-marker-error': {
      width: 5,
      background: 'red',
      cursor: 'default',
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
  const errors = data.errors;
  const t = data.t as ReturnType<typeof useTranslate>;
  return errors?.map((error) => {
    const location = error.location;
    const start = location?.start;
    const end = location?.end;

    const startColumn = start?.column - 1;
    const endColumn =
      start?.column === start?.column ? end?.column : end?.column - 1;
    const hint = {
      message: t(`parser_${error.message?.toLowerCase()}`, undefined, true),
      severity: 'error',
      type: 'validation',
      from: CodeMirror.Pos(start.line - 1, startColumn),
      to: CodeMirror.Pos(end.line - 1, endColumn),
    };
    return hint;
  });
}

CodeMirror.registerHelper('lint', 'icu', linter);

type Props = {
  value: string;
  onChange?: (val: string) => void;
  onSave?: (val: string) => void;
  onCmdSave?: (val: string) => void;
  onCancel?: () => void;
  background?: string;
  plaintext?: boolean;
  autofocus?: boolean;
  minHeight?: number | string;
  onBlur?: () => void;
};

export const Editor: React.FC<Props> = ({
  value,
  onChange,
  onCancel,
  onSave,
  onCmdSave,
  onBlur,
  plaintext,
  background,
  autofocus,
  minHeight = 100,
}) => {
  const classes = useStyles({ background, minHeight });
  const t = useTranslate();

  const handleChange = (val: string) => {
    onChange?.(val);
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
    autofocus,
    lineWrapping: true,
    keyMap: 'sublime',
    extraKeys: {
      Enter: (editor) => onSave?.(editor.getValue()),
      Esc: () => onCancel?.(),
      Tab: false,
      'Shift-Tab': false,
      'Ctrl-Enter': (editor) => onCmdSave?.(editor.getValue()),
      'Cmd-Enter': (editor) => onCmdSave?.(editor.getValue()),
    },
    gutters: ['CodeMirror-lint-markers'],
    lint: {
      // @ts-ignore
      errors: error ? [error] : [],
      t,
    },
    inputStyle: 'contenteditable',
    spellcheck: !plaintext,
  };

  return (
    <div data-cy="global-editor" className={classes.editor}>
      <CodeMirrorReact
        value={value}
        // @ts-ignore
        defineMode={{ name: 'icu', fn: icuMode }}
        options={options}
        onBeforeChange={(editor, data, value) => {
          handleChange(value);
        }}
        onBlur={() => onBlur?.()}
      />
    </div>
  );
};
