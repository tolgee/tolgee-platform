import { useCallback, useEffect, useRef } from 'react';
import { minimalSetup } from 'codemirror';
import { Compartment, EditorState, Prec } from '@codemirror/state';
import {
  EditorView,
  keymap,
  placeholder as cmPlaceholder,
  ViewUpdate,
} from '@codemirror/view';
import {
  autocompletion,
  CompletionContext,
  CompletionResult,
} from '@codemirror/autocomplete';
import { useTheme } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { getSuggestions, SuggestionItem } from './searchSuggestions';
import { QUALIFIER_HINTS } from './qualifierHints';
import { resolveSingleLineDoc, toSingleLineDoc } from './singleLineDoc';
import { buildCompletionTheme } from './searchFieldStyles';

const MIN_SUGGESTION_LETTERS = 2;

const placeholderExtension = (text: string) => [
  cmPlaceholder(text),
  EditorView.contentAttributes.of({ 'aria-label': text }),
];

type Params = {
  value: string;
  onSearchChange: (value: string) => void;
  setSearchOpen?: (open: boolean) => void;
  languageTags: string[];
  placeholder?: string;
};

/**
 * Owns the CodeMirror single-line search editor. Returns the ref to mount the
 * editor into; the caller only renders the surrounding chrome.
 */
export function useSearchEditor({
  value,
  onSearchChange,
  setSearchOpen,
  languageTags,
  placeholder,
}: Params) {
  const { t } = useTranslate();
  const theme = useTheme();
  const editorAreaRef = useRef<HTMLDivElement>(null);
  const editor = useRef<EditorView>();
  const placeholderCompartment = useRef<Compartment>();
  if (!placeholderCompartment.current) {
    placeholderCompartment.current = new Compartment();
  }

  const contextRef = useRef({ languageTags, onSearchChange, setSearchOpen, t });
  contextRef.current = { languageTags, onSearchChange, setSearchOpen, t };

  const hintText = useCallback((item: SuggestionItem) => {
    const { t } = contextRef.current;
    if (item.kind === 'language') {
      const hint = QUALIFIER_HINTS.language;
      // @tolgee-ignore
      return t(hint.keyName, hint.defaultValue, {
        tag: item.insert.slice(0, -1),
      });
    }
    const qualifier = item.insert.slice(0, -1) as keyof typeof QUALIFIER_HINTS;
    const hint = QUALIFIER_HINTS[qualifier];
    // @tolgee-ignore
    return hint && t(hint.keyName, hint.defaultValue);
  }, []);

  useEffect(() => {
    const suggestionSource = (
      context: CompletionContext
    ): CompletionResult | null => {
      const state = getSuggestions(
        context.state.doc.toString(),
        context.pos,
        contextRef.current.languageTags
      );
      if (!state) {
        return null;
      }
      if (state.replaceTo - state.replaceFrom < MIN_SUGGESTION_LETTERS) {
        return null;
      }
      return {
        from: state.replaceFrom,
        to: state.replaceTo,
        filter: false,
        options: state.items.map((item) => ({
          label: item.insert,
          detail: hintText(item),
        })),
      };
    };

    const instance = new EditorView({
      parent: editorAreaRef.current!,
      state: EditorState.create({
        doc: toSingleLineDoc(value),
        extensions: [
          minimalSetup,
          EditorState.transactionFilter.of((tr) => {
            const resolution = resolveSingleLineDoc(
              tr.startState.doc.toString(),
              tr.newDoc.toString(),
              tr.newDoc.lines
            );
            if (resolution.action === 'allow') {
              return tr;
            }
            if (resolution.action === 'reject') {
              return [];
            }
            return {
              changes: {
                from: 0,
                to: tr.startState.doc.length,
                insert: resolution.value,
              },
              selection: { anchor: resolution.value.length },
            };
          }),
          placeholderCompartment.current!.of(
            placeholderExtension(placeholder || '')
          ),
          autocompletion({
            override: [suggestionSource],
            icons: false,
            interactionDelay: 0,
          }),
          buildCompletionTheme(theme),
          Prec.low(
            keymap.of([
              {
                key: 'Escape',
                run: () => {
                  contextRef.current.onSearchChange('');
                  contextRef.current.setSearchOpen?.(false);
                  return true;
                },
              },
            ])
          ),
          EditorView.updateListener.of((update: ViewUpdate) => {
            if (update.docChanged) {
              contextRef.current.onSearchChange(update.state.doc.toString());
            }
          }),
        ],
      }),
    });
    editor.current = instance;
    return () => instance.destroy();
  }, [theme.palette.mode]);

  useEffect(() => {
    editor.current?.dispatch({
      effects: placeholderCompartment.current!.reconfigure(
        placeholderExtension(placeholder || '')
      ),
    });
  }, [placeholder]);

  useEffect(() => {
    const instance = editor.current;
    if (instance && instance.state.doc.toString() !== value) {
      instance.dispatch({
        changes: { from: 0, to: instance.state.doc.length, insert: value },
        selection: { anchor: value.length },
      });
    }
  }, [value]);

  return editorAreaRef;
}
