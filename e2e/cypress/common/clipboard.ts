export type ClipboardStub = { text: string };

/**
 * `copy-to-clipboard` copies the current selection with `execCommand`, falling back to
 * `window.prompt` when that fails — which it does outside the user gesture, e.g. after an
 * awaited request. Both are captured, either way the text is the same.
 *
 * `execCommand` must be forced to succeed rather than called through: the CI browser
 * refuses it, and on that path `copy()` returns false (the library never sets `success`
 * before prompting), so any assertion on the copied state fails there but not locally.
 */
export function stubClipboard(
  win: Window & Cypress.ApplicationWindow,
  clipboard: ClipboardStub = { text: '' }
): ClipboardStub {
  cy.stub(win, 'prompt').callsFake((_, input) => {
    clipboard.text = input;
  });

  const execCommand = win.document.execCommand.bind(win.document);
  cy.stub(win.document, 'execCommand').callsFake((command: string, ...args) => {
    if (command !== 'copy') {
      return execCommand(command, ...args);
    }
    clipboard.text = selectedText(win);
    return true;
  });

  return clipboard;
}

export function stubClipboardInCurrentWindow(): ClipboardStub {
  const clipboard: ClipboardStub = { text: '' };
  cy.window().then((win) => stubClipboard(win, clipboard));
  return clipboard;
}

function selectedText(win: Window): string {
  const selection = win.getSelection();
  if (!selection?.rangeCount) {
    return '';
  }
  // Selection.toString() returns the *rendered* text, whose line breaks shift between
  // Chrome versions; the copied node's textContent is the string copy() was handed.
  return selection.getRangeAt(0).commonAncestorContainer.textContent ?? '';
}
