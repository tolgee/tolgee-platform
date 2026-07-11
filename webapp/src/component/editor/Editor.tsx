import { RefObject, useEffect, useMemo, useRef } from 'react';
import { minimalSetup } from 'codemirror';
import { Compartment, EditorState, Prec, Extension } from '@codemirror/state';
import { EditorView, ViewUpdate, keymap, KeyBinding } from '@codemirror/view';
import { styled, useTheme } from '@mui/material';
import {
  tolgeeSyntax,
  PlaceholderPlugin,
  TolgeeHighlight,
  htmlIsolatesPlugin,
  generatePlaceholdersStyle,
  KeyNamePlugin,
  generateKeyNameStyle,
} from '@tginternal/editor';

import { Direction } from 'tg.fixtures/getLanguageDirection';
import { useScrollMargins } from 'tg.hooks/useScrollMargins';
import { visibleKeyNameSpacesPlugin } from './utils/codemirrorVisibleWhitespace';

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

  & .cm-tooltip {
    font-size: 12px;
    box-shadow: 1px 1px 6px rgba(0, 0, 0, 0.25);
    border-radius: 11px;
    border: none;
    color: ${({ theme }) => theme.palette.tooltip.text};
    background-color: ${({ theme }) => theme.palette.tooltip.background};
    padding: 4px 8px;
    margin-top: 4px;
  }

  & .cm-keyname-space-indicator {
    background-color: ${({ theme }) => theme.palette.label.lightBlue};
    border-radius: 2px;
  }
`;

export type EditorProps = {
  value: string;
  onChange?: (val: string) => void;
  background?: string;
  mode: 'placeholders' | 'syntax' | 'plain' | 'keyName';
  autofocus?: boolean;
  minHeight?: number | string;
  onBlur?: () => void;
  onFocus?: () => void;
  shortcuts?: KeyBinding[];
  scrollMargins?: Parameters<typeof useScrollMargins>[0];
  autoScrollIntoView?: boolean;
  direction?: Direction;
  locale?: string;
  editorRef?: React.RefObject<EditorView | null>;
  examplePluralNum?: number;
  nested?: boolean;
};

function useRefGroup<T>(value: T): RefObject<T> {
  const refObject = useRef(value);
  refObject.current = value;
  return refObject;
}

export const Editor: React.FC<React.PropsWithChildren<EditorProps>> = ({
  value,
  onChange,
  onFocus,
  onBlur,
  mode,
  autofocus,
  shortcuts,
  minHeight,
  direction,
  locale,
  editorRef,
  examplePluralNum,
  nested,
}) => {
  const ref = useRef<HTMLDivElement>(null);
  const editor = useRef<EditorView>();
  const placeholders = useRef<Compartment>(new Compartment());
  const editorTheme = useRef<Compartment>(new Compartment());
  const isolates = useRef<Compartment>(new Compartment());
  const keyBindings = useRef(shortcuts);
  const theme = useTheme();
  const callbacksRef = useRefGroup({
    onChange,
    onFocus,
    onBlur,
  });
  const languageCompartment = useRef<Compartment>(new Compartment());

  const StyledEditorWrapper = useMemo(() => {
    // Compose both placeholder and keyName styles unconditionally — switching
    // the wrapper based on `mode` would unmount the styled component on every
    // mode change and tear down the underlying CodeMirror EditorView with it,
    // breaking any test or interaction that toggles modes mid-edit.
    const withPlaceholders = generatePlaceholdersStyle({
      styled,
      colors: theme.palette.placeholders,
      component: StyledEditor,
    });
    return generateKeyNameStyle({
      styled,
      colors: theme.palette.placeholders.variant,
      component: withPlaceholders,
    });
  }, [theme.palette.placeholders]);

  keyBindings.current = shortcuts;

  useEffect(() => {
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
          EditorView.contentAttributes.of({
            spellcheck: 'true',
            dir: direction || 'ltr',
            lang: locale || '',
          }),
          languageCompartment.current.of([]),
          editorTheme.current.of([]),
          placeholders.current.of([]),
          isolates.current.of([]),
          direction === 'rtl' ? htmlIsolatesPlugin : [],
        ],
      }),
    });

    if (autofocus) {
      instance.focus();
    }

    editor.current = instance;
  }, [theme.palette.mode]);

  useEffect(() => {
    const placeholderPlugins: Extension[] = [];
    switch (mode) {
      case 'placeholders':
        placeholderPlugins.push(
          PlaceholderPlugin({
            examplePluralNum,
            nested: Boolean(nested),
            tooltips: true,
          })
        );
        break;
      case 'keyName':
        placeholderPlugins.push(KeyNamePlugin(), visibleKeyNameSpacesPlugin());
        break;
    }
    const syntaxPlugins =
      mode === 'plain' || mode === 'keyName'
        ? []
        : [tolgeeSyntax(Boolean(nested))];
    editor.current?.dispatch({
      selection: editor.current.state.selection,
      effects: [
        placeholders.current?.reconfigure(placeholderPlugins),
        languageCompartment.current.reconfigure(syntaxPlugins),
      ],
    });
  }, [mode, nested, examplePluralNum]);

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

  return (
    <StyledEditorWrapper
      data-cy="global-editor"
      ref={ref}
      key={theme.palette.mode}
      dir={direction}
      style={{
        minHeight,
        direction,
      }}
    />
  );
};
