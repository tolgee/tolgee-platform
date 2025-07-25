/// <reference types="cypress" />
import { getAnyContainingAriaLabelAttribute, getInput } from './xPath';
import { Scope } from './types';
import { waitForGlobalLoading } from './loading';
import { HOST } from './constants';
import Value = DataCy.Value;
import Chainable = Cypress.Chainable;

export const allScopes: Scope[] = [
  'keys.edit',
  'translations.edit',
  'translations.view',
];

export const clickAdd = () => {
  cy.xpath(getAnyContainingAriaLabelAttribute('add'))
    .should('be.visible')
    .click();
};

export const getPopover = () => {
  return cy
    .xpath("//*[contains(@class, 'MuiPopover-root')]")
    .filter(':visible')
    .should('be.visible');
};

export const gcy = (dataCy: Value, options?: Parameters<typeof cy.get>[1]) =>
  cy.get('[data-cy="' + dataCy + '"]', options);

export const gcyAdvanced = (
  { value, ...other }: { value: Value; [key: string]: string },
  options?: Parameters<typeof cy.get>[1]
) =>
  cy.get(
    `[data-cy="${value}"]${Object.entries(other)
      .map(([key, value]) => `[data-cy-${key}="${value}"]`)
      .join('')}`,
    options
  );

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
  return gcy('notistack-snackbar').should('contain', message);
};

export const assertNotMessage = (message: string) => {
  return gcy('notistack-snackbar').should('not.contain', message);
};

export const assertTooltip = (message: string) => {
  cy.xpath("//*[@role='tooltip']").should('contain', message);
};

export const selectInProjectMenu = (itemName: string) => {
  gcy('project-menu-items').get(`[aria-label="${itemName}"]`).click();
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
    // first select all
    getPopover()
      .get('input')
      .each(
        // cy.xpath(`.//*[text() = '${val}']/ancestor/ancestor::li//input`).each(
        ($input) => {
          const isChecked = $input.is(':checked');
          if (!isChecked) {
            cy.wrap($input).click();
          }
        }
      );

    // unselect necessary
    getPopover()
      .get('.MuiListItemText-primary')
      .each(($label) => {
        const labelText = $label.text();
        if (!renderedValues.includes(labelText)) {
          cy.wrap($label).click();
        }
      });
  });
  dismissMenu();
  waitForGlobalLoading();
};

export const assertMultiselect = (chainable: Chainable, values: string[]) => {
  chainable.find('div').first().click();

  getPopover().within(() => {
    getPopover()
      .get('li.MuiMenuItem-root')
      .each(($li) => {
        const labelText = $li.find('.MuiListItemText-primary').text();
        const input = cy.wrap($li).find('input');
        if (values.includes(labelText)) {
          input.should('be.checked');
        } else {
          input.should('not.be.checked');
        }
      });
  });
  dismissMenu();
};

export const getInputByName = (name: string): Chainable => {
  return cy.xpath(getInput(name));
};

export const switchToOrganizationWithSearch = (name: string): Chainable => {
  cy.gcy('organization-switch').click();
  cy.gcy('organization-switch-search').type(name);
  cy.waitForDom();

  gcy('organization-switch-item')
    .should('have.length', 1)
    .contains(name)
    .scrollIntoView()
    .click();
  return assertSwitchedToOrganization(name);
};

export const switchToOrganization = (name: string): Chainable => {
  cy.waitForDom();
  cy.gcy('organization-switch').click();
  cy.waitForDom();
  cy.gcy('organization-switch-item').contains(name).scrollIntoView().click();
  return assertSwitchedToOrganization(name);
};

export const assertSwitchedToOrganization = (name: string) => {
  cy.waitForDom();
  return cy.gcy('organization-switch').contains(name).should('be.visible');
};

export const visitProjectSettings = (projectId: number) => {
  return cy.visit(`${HOST}/projects/${projectId}/manage/edit`);
};

export const visitProjectLanguages = (projectId: number) => {
  return cy.visit(`${HOST}/projects/${projectId}/languages`);
};

export const visitProjectMembers = (projectId: number) => {
  return cy.visit(`${HOST}/projects/${projectId}/manage/permissions`);
};

export const visitProjectDashboard = (projectId: number) => {
  return cy.visit(`${HOST}/projects/${projectId}`);
};

export const visitProjectDeveloperContentDelivery = (projectId: number) => {
  return cy.visit(`${HOST}/projects/${projectId}/developer/content-delivery`);
};

export const visitProjectDeveloperStorage = (projectId: number) => {
  return cy.visit(`${HOST}/projects/${projectId}/developer/storage`);
};

export const visitProjectDeveloperHooks = (projectId: number) => {
  return cy.visit(`${HOST}/projects/${projectId}/developer/webhooks`);
};

export const dismissMenu = () => {
  cy.focused().type('{esc}');
};

export const assertMissingFeature = () => {
  gcy('disabled-feature-banner').should('be.visible');
};
