import { waitForGlobalLoading } from './loading';

export const IS_MAC = Boolean(navigator.userAgent.includes('Mac'));

export const editCell = (oldValue: string, newValue?: string, save = true) => {
  shortcut(['{enter}']);
  // wait for editor to appear
  cy.gcy('global-editor').should('be.visible');
  cy.contains(oldValue).should('be.visible');
  if (newValue) {
    // select all, delete and type new text
    cy.focused().type('{meta}a').type('{backspace}').type(newValue);

    if (save) {
      shortcut(['{enter}']);
      waitForGlobalLoading();
    }
  }
};

export function shortcut(keys: string[]) {
  let focused = cy.focused();
  keys.forEach((key, i) => {
    focused = focused.type(key, { release: i === keys.length - 1 });
  });
}

export function selectFirst() {
  cy.gcy('translations-table-cell').should('be.visible');
  cy.get('body').type('{downarrow}');
}

export function move(
  direction: 'downarrow' | 'uparrow' | 'leftarrow' | 'rightarrow',
  expectedText?: string
) {
  cy.focused().type(`{${direction}}`);
  if (expectedText) {
    cy.focused().contains(expectedText).should('be.visible');
  }
}

export function assertAvailableCommands(commands: string[]) {
  cy.gcy('translations-shortcuts-command').should(
    'have.length',
    commands.length
  );
  commands.forEach((command) => {
    cy.gcy('translations-shortcuts-command').contains(command).should('exist');
  });
}
