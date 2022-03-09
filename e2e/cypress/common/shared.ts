/// <reference types="cypress" />
import { getAnyContainingAriaLabelAttribute, getInput } from './xPath';
import { Scope } from './types';
import { waitForGlobalLoading } from './loading';
import Value = DataCy.Value;
import Chainable = Cypress.Chainable;

export const allScopes: Scope[] = [
  'keys.edit',
  'translations.edit',
  'translations.view',
];

export const clickAdd = () => {
  cy.wait(100);
  cy.xpath(getAnyContainingAriaLabelAttribute('add')).click();
};

export const getPopover = () => {
  return cy.xpath(
    "//*[contains(@class, 'MuiPopover-root') and not(contains(@style, 'visibility'))]"
  );
};

export const gcy = (dataCy: Value, options?: Parameters<typeof cy.get>[1]) =>
  cy.get('[data-cy="' + dataCy + '"]', options);

export const gcyChain = (...dataCy: Value[]) => {
  let xPath = '.';
  dataCy.forEach((dc) => {
    xPath += '//*[@data-cy="' + dc + '"]';
  });
  return cy.xpath(xPath);
};

export const goToPage = (page: number) =>
  gcy('global-list-pagination').within(() =>
    cy.xpath(".//button[text() = '" + page + "']").click()
  );
export const contextGoToPage = (chainable: Chainable, page: number) =>
  chainable
    .findDcy('global-list-pagination')
    .within(() => cy.xpath(".//button[text() = '" + page + "']").click());

export const clickGlobalSave = () => {
  gcy('global-form-save-button').click();
};

export const confirmHardMode = () => {
  gcy('global-confirmation-hard-mode-text-field').within(() => {
    cy.get('label')
      .then(($label) => {
        cy.get('input').type($label.text().replace('Rewrite text: ', ''));
      })
      .its('text');
  });
  gcy('global-confirmation-confirm').click();
};

export const confirmStandard = () => {
  gcy('global-confirmation-confirm').click();
};

export const assertMessage = (message: string) => {
  return gcy('global-snackbars').should('contain', message);
};

export const assertTooltip = (message: string) => {
  cy.xpath("//*[@role='tooltip']").should('contain', message);
};

export const selectInProjectMenu = (itemName: string) => {
  gcy('project-menu-items').contains(itemName).click();
};

export const selectInSelect = (chainable: Chainable, renderedValue: string) => {
  return chainable
    .find('div')
    .first()
    .click()
    .then(() => {
      getPopover().contains(renderedValue).click();
    });
};

export const toggleInMultiselect = (
  chainable: Chainable,
  renderedValues: string[]
) => {
  chainable.find('div').first().click();

  getPopover().within(() => {
    renderedValues.forEach((val) => {
      cy.xpath(`.//*[text() = '${val}']/ancestor::li//input`).each(($input) => {
        cy.wrap($input).click();
      });
    });

    cy.get('li').each(($li) => {
      const input = cy.wrap($li).find('input');
      input.each(($input) => {
        let isInValues = false;
        for (let i = 0; i < renderedValues.length; i++) {
          const val = renderedValues[i];
          if (
            document
              .evaluate(`.//*[text() = '${val}']`, $li.get(0))
              .iterateNext()
          ) {
            isInValues = true;
            break;
          }
        }
        const isChecked = $input.is(':checked');
        if ((isChecked && !isInValues) || (!isChecked && isInValues)) {
          input.click();
        }
      });
    });
  });
  cy.get('body').click(0, 0);
  waitForGlobalLoading();
};

export const getInputByName = (name: string): Chainable => {
  return cy.xpath(getInput(name));
};
