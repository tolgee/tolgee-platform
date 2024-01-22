import { useMemo, useRef } from 'react';
import CodeMirror from 'codemirror';
import { Controlled as CodeMirrorReact, DomEvent } from 'react-codemirror2';
import { parse } from '@formatjs/icu-messageformat-parser';
import { GlobalStyles, styled } from '@mui/material';
import 'codemirror/keymap/sublime';
import 'codemirror/lib/codemirror.css';
import 'codemirror/addon/lint/lint';
import 'codemirror/addon/lint/lint.css';

import icuMode from './icuMode';
import { useScrollMargins } from 'tg.hooks/useScrollMargins';
import { Direction } from 'tg.fixtures/getLanguageDirection';
import { useParserErrorTranslation } from 'tg.translationTools/useParserErrorTranslation';

const StyledWrapper = styled('div')<{
  minheight: string | number;
  background: string | undefined;
}>`
  display: grid;

  & .react-codemirror2 {
    display: grid;
    position: relative;
    align-self: stretch;
  }

  & .CodeMirror *::selection {
    background: ${({ theme }) =>
      theme.palette.mode === 'dark'
        ? theme.palette.emphasis[300]
        : theme.palette.emphasis[200]};
  }

  & .CodeMirror {
    width: 100%;
    min-height: ${({ minheight }) => minheight}px;
    height: 100%;
    margin-left: -5px;
    background: ${({ background, theme }) =>
      background || theme.palette.background.default};

    * {
      overflow: visible !important;
    }

    .CodeMirror-lines {
      padding: 0px !important;
    }

    .CodeMirror-line {
      padding: 0px !important;
      color: ${({ theme }) => theme.palette.editor.main} !important;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
        Helvetica, Arial, sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji',
        'Segoe UI Symbol' !important;
    }

    .CodeMirror-lint-markers {
      width: 5px;
    }

    .CodeMirror-gutters {
      border: 0px;
      background: transparent;
    }

    .CodeMirror-lint-marker-error {
      width: 4px;
      background: red;
      cursor: default;
      position: relative;
      top: -1px;
    }

    .cm-function {
      color: ${({ theme }) => theme.palette.editor.function};
    }

    .cm-string {
      color: ${({ theme }) => theme.palette.editor.main};
    }

    .cm-parameter {
      color: ${({ theme }) => theme.palette.editor.other};
    }

    .cm-option {
      color: ${({ theme }) => theme.palette.editor.other};
    }

    .cm-keyword {
      color: ${({ theme }) => theme.palette.editor.other};
    }

    .cm-bracket {
      color: ${({ theme }) => theme.palette.editor.other};
    }

    .cm-def {
      color: ${({ theme }) => theme.palette.editor.function};
    }
  }
`;

function linter(text: string, data: any) {
  const errors = data.errors;
  const translateParserError = data.translateParserError as ReturnType<
    typeof useParserErrorTranslation
  >;
  return errors?.map((error) => {
    const location = error.location;
    const start = location?.start;
    const end = location?.end;

    const startColumn = start?.column - 1;
    const endColumn =
      start?.column === start?.column ? end?.column : end?.column - 1;
    const hint = {
      message: translateParserError(error.message?.toLowerCase()),
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
  onInsertBase?: (val?: string) => void;
  onCancel?: () => void;
  background?: string;
  plaintext?: boolean;
  autofocus?: boolean;
  minHeight?: number | string;
  onBlur?: () => void;
  onFocus?: () => void;
  shortcuts?: CodeMirror.KeyMap;
  scrollMargins?: Parameters<typeof useScrollMargins>[0];
  autoScrollIntoView?: boolean;
  direction?: Direction;
  onKeyDown?: DomEvent;
};

export const Editor: React.FC<Props> = ({
  value,
  onChange,
  onCancel,
  onSave,
  onBlur,
  onFocus,
  plaintext,
  background,
  autofocus,
  minHeight = 100,
  shortcuts,
  scrollMargins,
  autoScrollIntoView,
  direction = 'ltr',
  onKeyDown,
}) => {
  const wrapperRef = useRef<HTMLDivElement>(null);
  const translateParserError = useParserErrorTranslation();

  const handleChange = (val: string) => {
    onChange?.(val);
  };

  const error = useMemo(() => {
    try {
      parse(value, { captureLocation: true, ignoreTag: true });
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
      End: 'goLineRight',
      Home: 'goLineLeft',
      ...shortcuts,
    },
    gutters: ['CodeMirror-lint-markers'],
    lint: {
      // @ts-ignore
      errors: error ? [error] : [],
      translateParserError,
    },
    inputStyle: 'contenteditable',
    spellcheck: !plaintext,
    direction: direction,
  };

  const wrapperScrollMargins = useScrollMargins(scrollMargins);

  return (
    <>
      <GlobalStyles
        styles={(theme) => ({
          '.CodeMirror-lint-tooltip': {
            background: theme.palette.emphasis[100] + ' !important',
            borderRadius: '0px !important',
            zIndex: theme.zIndex.tooltip + ' !important',
            color: theme.palette.text.primary + ' !important',
          },
          '.CodeMirror-lint-message-error': {
            backgroundImage: 'unset !important',
            paddingLeft: '0px !important',
          },
        })}
      />
      <StyledWrapper
        background={background}
        minheight={minHeight}
        data-cy="global-editor"
        style={scrollMargins ? wrapperScrollMargins : undefined}
        ref={wrapperRef}
      >
        <CodeMirrorReact
          value={value}
          // @ts-ignore
          defineMode={{ name: 'icu', fn: icuMode }}
          options={options}
          onBeforeChange={(editor, data, value) => {
            handleChange(value);
          }}
          onKeyDown={(...params) => onKeyDown?.(...params)}
          onBlur={() => onBlur?.()}
          onFocus={(e) => {
            onFocus?.();
            if (autoScrollIntoView) {
              wrapperRef.current?.scrollIntoView({
                behavior: 'smooth',
                block: 'nearest',
                inline: 'nearest',
              });
            }
          }}
        />
      </StyledWrapper>
    </>
  );
};
