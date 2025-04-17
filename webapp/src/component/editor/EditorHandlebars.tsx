import { RefObject, useEffect, useRef } from 'react';
import { minimalSetup } from 'codemirror';
import { EditorState, Prec } from '@codemirror/state';
import { EditorView, ViewUpdate, keymap, KeyBinding } from '@codemirror/view';
import { styled, useTheme } from '@mui/material';
import { htmlIsolatesPlugin } from '@tginternal/editor';
import { handlebarsLanguage } from '@xiechao/codemirror-lang-handlebars';
import { autocompletion } from '@codemirror/autocomplete';

import { Direction } from 'tg.fixtures/getLanguageDirection';
import { useScrollMargins } from 'tg.hooks/useScrollMargins';
import { handlebarsAutocomplete } from './utils/handlebarsAutocomplete';
import { components } from 'tg.service/apiSchema.generated';
import { handlebarsTooltip } from './utils/handlebarsTooltip';
import {
  EditorError,
  errorPlugin,
  setErrorsEffect,
} from './utils/codemirrorError';
import { useTranslate } from '@tolgee/react';

type PromptVariable = components['schemas']['PromptVariableDto'];

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
    padding: 10px 14px;
    margin-top: 4px;
    max-width: 100%;
    display: block;
    unicode-bidi: embed;
    white-space: pre-wrap;
  }

  & .cm-tooltip .header {
    display: flex;
    justify-content: space-between;
  }

  & .cm-tooltip .title {
    font-size: 16px;
    font-weight: 500;
  }

  & .cm-tooltip .action {
    display: flex;
    justify-content: space-between;
    align-items: center;
    cursor: pointer;
    gap: 4px;
    color: ${({ theme }) => theme.palette.primary.main};
    font-size: 13px;
    text-transform: uppercase;
    font-weight: 500;
    padding: 4px 8px;
    border-radius: 4px;
    border: 1px solid
      ${({ theme }) => theme.palette.tokens.primary._states.outlinedBorder};
  }

  .cm-error-underline {
    text-decoration: underline wavy red;
    text-underline-offset: 4px;
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
  background?: string;
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
  availableVariables?: PromptVariable[];
  unknownVariableMessage?: string;
  errors?: EditorError[];
};

export const EditorHandlebars: React.FC<EditorProps> = ({
  value,
  onChange,
  onFocus,
  onBlur,
  autofocus,
  shortcuts,
  minHeight,
  direction,
  locale,
  editorRef,
  availableVariables,
  unknownVariableMessage,
  errors,
}) => {
  const ref = useRef<HTMLDivElement>(null);
  const editor = useRef<EditorView>();
  const keyBindings = useRef(shortcuts);
  const variableRefs = useRef(availableVariables);
  const unknownVariableMessageRef = useRef(unknownVariableMessage);
  const theme = useTheme();
  const callbacksRef = useRefGroup({
    onChange,
    onFocus,
    onBlur,
  });
  const { t } = useTranslate();

  keyBindings.current = shortcuts;
  variableRefs.current = availableVariables;
  unknownVariableMessageRef.current = unknownVariableMessage;

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
          handlebarsLanguage,
          autocompletion({
            override: [handlebarsAutocomplete(variableRefs)],
            icons: false,
            activateOnCompletion: (c) => c.type === 'object',
            activateOnTyping: true,
          }),
          errorPlugin(),
          handlebarsTooltip(variableRefs, unknownVariableMessageRef, t),
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
    editor.current?.dispatch({
      effects: setErrorsEffect.of(errors ?? []),
    });
  }, [editor.current, errors]);

  useEffect(() => {
    const state = editor.current?.state;
    const editorValue = state?.doc.toString();
    if (state && editorValue !== value) {
      const transaction = state.update({
        changes: { from: 0, to: state.doc.length, insert: value || '' },
      });
      editor.current?.update([transaction]);
    }
  }, [editor.current, value]);

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
    <>
      <StyledEditor
        data-cy="handlebars-editor"
        ref={ref}
        key={theme.palette.mode}
        dir={direction}
        style={{
          minHeight,
          direction,
        }}
      />
    </>
  );
};
