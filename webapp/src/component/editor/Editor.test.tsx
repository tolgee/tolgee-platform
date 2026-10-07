import { act, useLayoutEffect, useState } from 'react';
import { createRoot, Root } from 'react-dom/client';
import { ThemeProvider } from '@mui/material';
import { EditorView } from '@codemirror/view';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { getTheme } from '../../ThemeProvider';
import { Editor } from './Editor';

vi.mock('tg.component/InvisibleCharacter', () => ({
  useInvisibleCharacterLabel: () => () => '',
}));

const typeText = (view: EditorView, text: string) => {
  const position = view.state.doc.length;
  view.dispatch({
    changes: { from: position, insert: text },
    selection: { anchor: position + text.length },
  });
};

describe('Editor value sync', () => {
  let container: HTMLDivElement;
  let root: Root;
  let setParentValue: (value: string) => void;
  let onCommit: ((value: string) => void) | undefined;
  const editorRef = { current: null as EditorView | null };
  const theme = getTheme('light');

  const Parent = () => {
    const [value, setValue] = useState('');
    setParentValue = setValue;
    useLayoutEffect(() => {
      onCommit?.(value);
    }, [value]);
    return (
      <ThemeProvider theme={theme}>
        <Editor
          mode="keyName"
          value={value}
          onChange={setValue}
          editorRef={editorRef}
        />
      </ThemeProvider>
    );
  };

  beforeEach(() => {
    // @ts-ignore
    globalThis.IS_REACT_ACT_ENVIRONMENT = true;
    onCommit = undefined;
    container = document.createElement('div');
    document.body.appendChild(container);
    root = createRoot(container);
    act(() => root.render(<Parent />));
  });

  afterEach(() => {
    act(() => root.unmount());
    container.remove();
  });

  it('keeps text typed before the editor syncs the previous value', async () => {
    const view = editorRef.current!;
    const contentHistory: string[] = [];
    const originalUpdate = view.update.bind(view);
    vi.spyOn(view, 'update').mockImplementation((transactions) => {
      originalUpdate(transactions);
      contentHistory.push(view.state.doc.toString());
    });
    // The next key arrives after React commits the previous value,
    // but before the editor effect for that value runs.
    onCommit = (value) => {
      if (value === 'n') {
        typeText(view, 'e');
      }
    };

    await act(async () => typeText(view, 'n'));

    expect(contentHistory).toEqual(['n', 'ne']);
  });

  it('applies a parent value that was shown before', () => {
    const view = editorRef.current!;

    act(() => setParentValue('A'));
    act(() => setParentValue('B'));
    act(() => setParentValue('A'));

    expect(view.state.doc.toString()).toBe('A');
  });

  it('applies a value changed by the parent', () => {
    const view = editorRef.current!;

    act(() => typeText(view, 'abc'));
    act(() => setParentValue('external'));

    expect(view.state.doc.toString()).toBe('external');
  });
});
