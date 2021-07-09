import { useRef } from 'react';
import MonacoEditor, {
  BeforeMount,
  OnChange,
  OnMount,
  loader,
} from '@monaco-editor/react';
import * as monaco from 'monaco-editor';
import completion from './icuCompletion';
import tokenizer from './icuTokenizer';
import icuValidator from './icuValidator';
import icuTheme from './icuTheme';
import { makeStyles } from '@material-ui/core';
import { useState } from 'react';
import { useEffect } from 'react';

loader.init().then((monaco) => {
  monaco.languages.register({ id: 'icu' });
  monaco.languages.setMonarchTokensProvider('icu', tokenizer);
  monaco.editor.onDidCreateModel(icuValidator(monaco.editor));
});

const useStyles = makeStyles((theme) => ({
  wrapper: {
    '& .overflowingContentWidgets *': {
      zIndex: theme.zIndex.tooltip,
    },
  },
}));

type Props = {
  initialValue: string;
  variables: string[];
  minHeight?: number;
  width?: string;
  onChange?: OnChange;
  onSave?: (nextDirection: DirectionType) => void;
  onCancel?: () => void;
  autoFocus?: boolean;
  language?: 'icu' | 'plaintext';
};

export type DirectionType = 'UP' | 'DOWN' | 'LEFT' | 'RIGHT';

export const Editor = ({
  variables,
  initialValue,
  minHeight = 20,
  width,
  onChange,
  onSave,
  onCancel,
  autoFocus,
  language = 'icu',
}: Props) => {
  const [dynamicHeight, setDynamicHeight] = useState(
    minHeight as string | number | undefined
  );

  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor>();
  const onSaveRef = useRef<typeof onSave>();
  onSaveRef.current = onSave;
  const disposeRef = useRef<() => void>();

  const beforeMount: BeforeMount = (monaco) => {
    const { dispose } = monaco.languages.registerCompletionItemProvider(
      'icu',
      completion({
        variables,
        enableSnippets: true,
      })
    );
    disposeRef.current = dispose;
  };

  useEffect(() => {
    return () => disposeRef.current?.();
  }, []);

  const onMount: OnMount = (editor, monaco) => {
    editorRef.current = editor;
    monaco.editor.defineTheme('icuTheme', icuTheme);
    monaco.editor.setTheme('icuTheme');
    editor.addAction({
      id: 'save-data-down',
      label: 'Save data',
      keybindings: [monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter],
      run: () => onSaveRef.current?.('DOWN'),
    });
    editor.addAction({
      id: 'save-data-up',
      label: 'Save data',
      keybindings: [
        monaco.KeyMod.CtrlCmd | monaco.KeyMod.Shift | monaco.KeyCode.Enter,
      ],
      run: () => onSaveRef.current?.('UP'),
    });
    editor.addAction({
      id: 'cancel-edit',
      label: 'Cancel edit',
      keybindings: [monaco.KeyCode.Escape],
      run: () => onCancel?.(),
    });
    if (autoFocus) {
      editor.focus();
    }
  };

  const handleChange: OnChange = (...attr) => {
    onChange?.(...attr);
    const realHeight = editorRef.current?.getContentHeight();
    setDynamicHeight(
      realHeight && realHeight > minHeight ? realHeight : minHeight
    );
  };

  const classes = useStyles();

  return (
    <div className={classes.wrapper}>
      <MonacoEditor
        defaultValue={initialValue}
        defaultLanguage={language}
        height={dynamicHeight}
        loading={<></>}
        options={{
          lineNumbers: 'off',
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          theme: 'icuTheme',
          renderValidationDecorations: 'on',
          contextmenu: false,
          scrollbar: {
            alwaysConsumeMouseWheel: false,
            vertical: 'hidden',
            verticalScrollbarSize: 8,
          },
          overviewRulerBorder: false,
          hideCursorInOverviewRuler: true,
          overviewRulerLanes: 0,
          folding: false,
          wordBasedSuggestions: false,
          fixedOverflowWidgets: true,
          wordWrap: 'on',
          padding: { top: 0, bottom: 0 },
          glyphMargin: false,
          lineDecorationsWidth: 0,
          lineNumbersMinChars: 0,
          automaticLayout: true,
        }}
        beforeMount={beforeMount}
        onMount={onMount}
        onChange={handleChange}
      />
    </div>
  );
};
