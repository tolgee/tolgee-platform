export type Clipboard = { text: string };

/**
 * `copy-to-clipboard` copies via `execCommand`, but falls back to `prompt` when that
 * fails — which it does whenever the copy happens outside the user gesture, e.g. after
 * an awaited request. Both paths are stubbed so callers don't have to care which one
 * their component takes.
 */
export function stubClipboard(
  win: Window & Cypress.ApplicationWindow,
  clipboard: Clipboard = { text: '' }
): Clipboard {
  cy.stub(win, 'prompt').callsFake((_, input) => {
    clipboard.text = input;
  });

  const execCommand = win.document.execCommand.bind(win.document);
  cy.stub(win.document, 'execCommand').callsFake((command: string, ...args) => {
    if (command !== 'copy') {
      return execCommand(command, ...args);
    }
    clipboard.text = win.getSelection()?.toString() ?? '';
    return true;
  });

  return clipboard;
}

/** Same, for a page that is already loaded. */
export function stubClipboardNow(): Clipboard {
  const clipboard: Clipboard = { text: '' };
  cy.window().then((win) => stubClipboard(win, clipboard));
  return clipboard;
}
