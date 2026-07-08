import { useEffect, useMemo, useRef } from 'react';
import { minimalSetup } from 'codemirror';
import { EditorState, Prec } from '@codemirror/state';
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
import { IconButton, styled, Theme, useTheme } from '@mui/material';
import { SearchSm, XClose } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';

import { stopAndPrevent } from 'tg.fixtures/eventHandler';

import { getSuggestions, SuggestionItem } from './searchSuggestions';
import { SearchSyntaxHelp } from '../SearchSyntaxHelp';

const MIN_SUGGESTION_LETTERS = 2;

const StyledRoot = styled('div')`
  display: flex;
  align-items: center;
  gap: 6px;
  height: 40px;
  padding: 0px 7px 0px 12px;
  border: 1px solid ${({ theme }) => theme.palette.tokens.border.primary};
  border-radius: 4px;
  background: ${({ theme }) => theme.palette.background.default};
  color: ${({ theme }) => theme.palette.text.primary};

  &:hover {
    border-color: ${({ theme }) => theme.palette.text.primary};
  }

  &:focus-within {
    border-color: ${({ theme }) => theme.palette.primary.main};
    outline: 1px solid ${({ theme }) => theme.palette.primary.main};
    outline-offset: -2px;
  }

  & .cm-editor {
    outline: none;
    width: 100%;
  }

  & .cm-scroller {
    overflow-x: hidden;
    font-family: ${({ theme }) => theme.typography.fontFamily};
  }

  & .cm-line {
    font-size: ${({ theme }) => theme.typography.body2.fontSize};
    padding: 0px;
  }

  & .cm-content {
    padding: 0px;
    caret-color: ${({ theme }) => theme.palette.text.primary};
  }

  & .cm-placeholder {
    color: ${({ theme }) => theme.palette.text.secondary};
  }

  & .cm-cursor {
    border-color: ${({ theme }) => theme.palette.text.primary};
  }
`;

const StyledEditorArea = styled('div')`
  display: grid;
  flex-grow: 1;
  min-width: 0px;
`;

const StyledSearchIcon = styled(SearchSm)`
  flex-shrink: 0;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const buildCompletionTheme = (theme: Theme) =>
  EditorView.theme(
    {
      '.cm-tooltip.cm-tooltip-autocomplete': {
        borderRadius: '8px',
        border: `1px solid ${theme.palette.tokens.border.secondary}`,
        background: theme.palette.background.paper,
        boxShadow: theme.shadows[3],
        padding: '4px 0px',
        overflow: 'hidden',
      },
      '.cm-tooltip.cm-tooltip-autocomplete > ul': {
        fontFamily: theme.typography.fontFamily as string,
      },
      '.cm-tooltip.cm-tooltip-autocomplete > ul > li': {
        padding: '6px 12px',
        color: theme.palette.text.primary,
      },
      '.cm-tooltip.cm-tooltip-autocomplete > ul > li[aria-selected]': {
        background: theme.palette.primary.main,
        color: theme.palette.primary.contrastText,
      },
      '.cm-tooltip.cm-tooltip-autocomplete > ul > li[aria-selected] .cm-completionDetail':
        {
          color: theme.palette.primary.contrastText,
        },
      '.cm-completionLabel': {
        fontFamily: 'monospace',
      },
      '.cm-completionDetail': {
        fontStyle: 'normal',
        color: theme.palette.text.secondary,
        marginLeft: '12px',
      },
    },
    { dark: theme.palette.mode === 'dark' }
  );

type Props = {
  value: string;
  onSearchChange: (value: string) => void;
  setSearchOpen?: (open: boolean) => void;
  languageTags: string[];
  placeholder?: string;
  className?: string;
  style?: React.CSSProperties;
};

export const TranslationsSearchField = (props: Props) => {
  const {
    value,
    onSearchChange,
    setSearchOpen,
    languageTags,
    placeholder,
    className,
    style,
  } = props;
  const { t } = useTranslate();
  const theme = useTheme();
  const editorAreaRef = useRef<HTMLDivElement>(null);
  const editor = useRef<EditorView>();

  const contextRef = useRef({ languageTags, onSearchChange, setSearchOpen, t });
  contextRef.current = { languageTags, onSearchChange, setSearchOpen, t };

  const hintText = useMemo(
    () =>
      function hintText(item: SuggestionItem) {
        const { t } = contextRef.current;
        if (item.kind === 'language') {
          return t(
            'translations_search_help_language',
            'Search only in one language'
          );
        }
        const hints: Record<string, string> = {
          'key:': t(
            'translations_search_suggestion_key',
            'Search only in key names'
          ),
          'description:': t(
            'translations_search_help_description',
            'Search only in key descriptions'
          ),
          'namespace:': t(
            'translations_search_help_namespace',
            'Search only in namespaces'
          ),
          'translation:': t(
            'translations_search_help_translation',
            'Search only in translation texts'
          ),
        };
        return hints[item.insert];
      },
    []
  );

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
        doc: value,
        extensions: [
          minimalSetup,
          // reject any transaction introducing a line break — single-line field
          EditorState.transactionFilter.of((tr) =>
            tr.newDoc.lines > 1 ? [] : tr
          ),
          cmPlaceholder(placeholder || ''),
          autocompletion({
            override: [suggestionSource],
            icons: false,
            // Enter has no other meaning in this single-line field, so the
            // accidental-accept protection only makes Enter feel unresponsive
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
    const instance = editor.current;
    if (instance && instance.state.doc.toString() !== value) {
      instance.dispatch({
        changes: { from: 0, to: instance.state.doc.length, insert: value },
        selection: { anchor: value.length },
      });
    }
  }, [value]);

  return (
    <StyledRoot
      className={className}
      style={style}
      data-cy="global-search-field"
    >
      <StyledSearchIcon width={20} height={20} />
      <StyledEditorArea ref={editorAreaRef} />
      {Boolean(value) && (
        <IconButton
          size="small"
          onClick={stopAndPrevent(() => onSearchChange(''))}
          onMouseDown={stopAndPrevent()}
        >
          <XClose width={20} height={20} />
        </IconButton>
      )}
      <SearchSyntaxHelp />
    </StyledRoot>
  );
};
