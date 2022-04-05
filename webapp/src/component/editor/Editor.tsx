import { useMemo, useRef } from 'react';
import { useTranslate } from '@tolgee/react';
import CodeMirror from 'codemirror';
import { Controlled as CodeMirrorReact } from 'react-codemirror2-react-17';
import { parse } from '@formatjs/icu-messageformat-parser';
import { styled, GlobalStyles } from '@mui/material';
import 'codemirror/keymap/sublime';
import 'codemirror/lib/codemirror.css';
import 'codemirror/addon/lint/lint';
import 'codemirror/addon/lint/lint.css';

import icuMode from './icuMode';
import { useScrollMargins } from 'tg.hooks/useScrollMargins';

export type Direction = 'DOWN';

const StyledWrapper = styled('div')<{
  minheight: string | number;
  background: string | undefined;
}>`
  display: flex;
  flex-grow: 1;
  align-items: stretch;
  & .react-codemirror2 {
    display: flex;
    flex-grow: 1;
    position: relative;
  }
  & .CodeMirror {
    width: 100%;
    min-height: ${({ minheight }) => minheight}px;
    height: 100%;
    margin-left: -5px;
    background: ${({ background }) => background};

    * {
      overflow: visible !important;
    }

    .CodeMirror-lines {
      padding: 0px !important;
    }

    .CodeMirror-line {
      padding: 0px !important;
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
      color: #007300;
    }
    .cm-parameter {
      color: #002bff;
    }
    .cm-option {
      color: #002bff;
    }
    .cm-keyword {
      color: #002bff;
    }
    .cm-string {
      color: #000000;
    }
    .cm-bracket {
      color: #002bff;
    }
  }
`;

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
}) => {
  const wrapperRef = useRef<HTMLDivElement>(null);
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
      ...shortcuts,
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

  const wrapperScrollMargins = useScrollMargins(scrollMargins);

  return (
    <>
      <GlobalStyles
        styles={(theme) => ({
          '.CodeMirror-lint-tooltip': {
            background: 'white !important',
            borderRadius: '0px !important',
            zIndex: theme.zIndex.tooltip + ' !important',
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
