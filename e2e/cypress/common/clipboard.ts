export type ClipboardStub = { text: string };

/**
 * Stubs both clipboard copy methods used by frontend and captures copied text:
 * - `window.prompt(text)`
 * - `document.execCommand('copy')`
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
  return selection.getRangeAt(0).commonAncestorContainer.textContent ?? '';
}
