import { EditorView } from '@codemirror/view';
import { styled, Theme } from '@mui/material';
import { SearchSm } from '@untitled-ui/icons-react';

export const StyledRoot = styled('div')`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.75)};
  height: 40px;
  padding: ${({ theme }) => theme.spacing(0, 1, 0, 1.5)};
  border: 1px solid ${({ theme }) => theme.palette.tokens.border.primary};
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
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

export const StyledEditorArea = styled('div')`
  display: grid;
  flex-grow: 1;
  min-width: 0px;
`;

export const StyledSearchIcon = styled(SearchSm)`
  flex-shrink: 0;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

export const buildCompletionTheme = (theme: Theme) =>
  EditorView.theme(
    {
      '.cm-tooltip.cm-tooltip-autocomplete': {
        borderRadius: `${theme.shape.borderRadius}px`,
        border: `1px solid ${theme.palette.tokens.border.secondary}`,
        background: theme.palette.background.paper,
        boxShadow: theme.shadows[3],
        padding: theme.spacing(0.5, 0),
        overflow: 'hidden',
      },
      '.cm-tooltip.cm-tooltip-autocomplete > ul': {
        fontFamily: theme.typography.fontFamily as string,
      },
      '.cm-tooltip.cm-tooltip-autocomplete > ul > li': {
        padding: theme.spacing(0.75, 1.5),
        color: theme.palette.text.primary,
      },
      '.cm-tooltip.cm-tooltip-autocomplete > ul > li[aria-selected]': {
        background: theme.palette.tokens.text._states.hover,
        color: theme.palette.text.primary,
      },
      '.cm-completionLabel': {
        fontFamily: 'monospace',
      },
      '.cm-completionDetail': {
        fontStyle: 'normal',
        color: theme.palette.text.secondary,
        marginLeft: theme.spacing(1.5),
      },
    },
    { dark: theme.palette.mode === 'dark' }
  );
