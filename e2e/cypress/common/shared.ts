/// <reference types="cypress" />
import {
  getAnyContainingAriaLabelAttribute,
  getInput,
  getTextArea,
} from './xPath';
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

const LANGUAGE_ITEM = 'li[data-cy="translations-language-select-item"]';

const languageLabel = ($li: JQuery) =>
  $li.find('.MuiListItemText-primary').text();

const setLanguageItem = (index: number, checked: boolean) =>
  cy
    .get(LANGUAGE_ITEM)
    .eq(index)
    .find('input')
    .then(($input) => {
      if ($input.is(':checked') !== checked) {
        cy.get(LANGUAGE_ITEM).eq(index).click();
      }
    });

const assertLanguageItems = (values: string[]) =>
  cy.get(LANGUAGE_ITEM).each(($li) => {
    cy.wrap($li)
      .find('input')
      .should(
        values.includes(languageLabel($li)) ? 'be.checked' : 'not.be.checked'
      );
  });

export const toggleInMultiselect = (
  chainable: Chainable,
  renderedValues: string[]
) => {
  chainable.find('div').first().click();

  getPopover().within(() => {
    cy.get(LANGUAGE_ITEM).then(($items) => {
      const labels = $items.map((_, li) => languageLabel(Cypress.$(li))).get();
      const indexes = labels.map((_, index) => index);
      const wanted = (index: number) => renderedValues.includes(labels[index]);

      // Removing the last remaining language is refused with only a snackbar, so everything that
      // should end up selected is clicked before anything is deselected.
      indexes.filter(wanted).forEach((index) => setLanguageItem(index, true));
      indexes
        .filter((index) => !wanted(index))
        .forEach((index) => setLanguageItem(index, false));
    });

    assertLanguageItems(renderedValues);
  });
  dismissMenu();
  waitForGlobalLoading();
};

export const assertMultiselect = (chainable: Chainable, values: string[]) => {
  chainable.find('div').first().click();
  getPopover().within(() => assertLanguageItems(values));
  dismissMenu();
};

export const getInputByName = (name: string): Chainable => {
  return cy.xpath(getInput(name));
};

export const getTextAreaByName = (name: string): Chainable => {
  return cy.xpath(getTextArea(name));
};

export const switchToOrganizationWithSearch = (name: string): Chainable => {
  cy.gcy('organization-switch').click();
  cy.gcy('switch-popover-search').type(name);
  cy.waitForDom();

  gcy('switch-popover-item')
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
  cy.gcy('switch-popover-item').contains(name).scrollIntoView().click();
  return assertSwitchedToOrganization(name);
};

export const assertSwitchedToOrganization = (name: string) => {
  cy.waitForDom();
  return cy.gcy('organization-switch').contains(name).should('be.visible');
};

export const openPublicProject = (name: string) => {
  cy.visit(`${HOST}/public-projects`);
  waitForGlobalLoading();
  gcyAdvanced({ value: 'dashboard-projects-list-item', name }).click();
  cy.url().should('match', /\/projects\/[0-9]+/);
  waitForGlobalLoading();
};

export const visitProjectSettings = (projectId: number) => {
  return cy.visit(`${HOST}/projects/${projectId}/manage/edit`);
};

export const visitProjectSettingsAdvanced = (projectId: number) => {
  return cy.visit(`${HOST}/projects/${projectId}/manage/edit/advanced`);
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
