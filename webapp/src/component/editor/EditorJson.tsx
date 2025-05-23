import { RefObject, useEffect, useMemo, useRef } from 'react';
import { minimalSetup } from 'codemirror';
import { Compartment, EditorState, Prec } from '@codemirror/state';
import { EditorView, ViewUpdate, keymap, KeyBinding } from '@codemirror/view';
import { GlobalStyles, css, styled, useTheme } from '@mui/material';
import { json, jsonLanguage, jsonParseLinter } from '@codemirror/lang-json';
import { linter } from '@codemirror/lint';
import { TolgeeHighlight } from '@tginternal/editor';

const StyledEditor = styled('div')`
  font-size: 14px;
  display: grid;

  & .cm-editor {
    outline: none;
  }

  & .cm-selectionBackground {
    background: ${({ theme }) =>
      theme.palette.mode === 'dark'
        ? theme.palette.emphasis[400]
        : theme.palette.emphasis[200]} !important;
  }

  & .cm-line {
    font-size: 15px;
    font-family: ${({ theme }) => theme.typography.fontFamily};
    padding: 0px 1px;
  }

  & .cm-content {
    padding: 0px;
  }

  & .cm-cursor {
    border-color: ${({ theme }) => theme.palette.text.primary};
  }
`;

function useRefGroup<T>(value: T): RefObject<T> {
  const refObject = useRef(value);
  refObject.current = value;
  return refObject;
}

export type EditorProps = {
  value: string;
  onChange?: (val: string) => void;
  onBlur?: () => void;
  onFocus?: () => void;
  minHeight?: number | string;
  autofocus?: boolean;
  shortcuts?: KeyBinding[];
  editorRef?: React.RefObject<EditorView | null>;
};

export const EditorJson: React.FC<EditorProps> = ({
  value,
  onChange,
  onFocus,
  onBlur,
  minHeight,
  autofocus,
  shortcuts,
  editorRef,
}) => {
  const ref = useRef<HTMLDivElement>(null);
  const editor = useRef<EditorView>();
  const keyBindings = useRef(shortcuts);
  const editorTheme = useRef<Compartment>(new Compartment());
  const theme = useTheme();
  const callbacksRef = useRefGroup({
    onChange,
    onFocus,
    onBlur,
  });

  keyBindings.current = shortcuts;

  useEffect(() => {
    const languageCompartment = new Compartment();

    const shortcutsUptoDate = shortcuts?.map((value, i) => {
      return {
        ...value,
        run: (val: EditorView) => keyBindings.current?.[i].run?.(val) ?? false,
      };
    });

    const instance = new EditorView({
      parent: ref.current!,
      state: EditorState.create({
        doc: value,
        extensions: [
          minimalSetup,
          Prec.highest(keymap.of(shortcutsUptoDate ?? [])),
          EditorView.lineWrapping,
          EditorView.updateListener.of((v: ViewUpdate) => {
            if (v.focusChanged) {
              if (v.view.hasFocus) {
                callbacksRef.current?.onFocus?.();
              } else {
                callbacksRef.current?.onBlur?.();
              }
            }
            if (v.docChanged) {
              callbacksRef.current?.onChange?.(v.state.doc.toString());
            }
          }),
          editorTheme.current.of([]),
          languageCompartment.of([json(), jsonLanguage]),
          jsonLanguage,
          linter(jsonParseLinter()),
        ],
      }),
    });

    if (autofocus) {
      instance.focus();
    }

    editor.current = instance;
  }, [theme.palette.mode]);

  useEffect(() => {
    const state = editor.current?.state;
    const editorValue = state?.doc.toString();
    if (state && editorValue !== value) {
      const transaction = state.update({
        changes: { from: 0, to: state.doc.length, insert: value || '' },
      });
      editor.current?.update([transaction]);
    }
  }, [value]);

  useEffect(() => {
    editor.current?.dispatch({
      effects: editorTheme.current?.reconfigure([
        TolgeeHighlight(theme.palette.editor),
      ]),
    });
  }, [theme.palette.mode]);

  useEffect(() => {
    // set cursor to the end of document
    const length = editor.current!.state.doc.length;
    editor.current!.dispatch({ selection: { anchor: length } });

    return () => {
      editor.current!.destroy();
    };
  }, []);

  useEffect(() => {
    if (editorRef) {
      // @ts-ignore
      editorRef.current = editor.current;
    }
  });

  const globalStyles = useMemo(
    () => (
      <GlobalStyles
        styles={css`
          .cm-tooltip-lint {
            background: ${theme.palette.background.paper} !important;
            border-radius: 0px !important;
            z-index: ${theme.zIndex.tooltip} !important;
            color: ${theme.palette.text.primary} !important;
          }

          .cm-diagnostic {
            background-image: unset !important;
            border-left: 0px !important;
          }
        `}
      />
    ),
    [theme]
  );

  return (
    <>
      {globalStyles}
      <StyledEditor
        key={theme.palette.mode}
        ref={ref}
        style={{
          minHeight,
        }}
      />
    </>
  );
};
