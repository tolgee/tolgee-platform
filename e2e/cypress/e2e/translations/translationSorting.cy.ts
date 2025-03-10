import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { createKey } from '../../common/apiCalls/common';
import { waitForGlobalLoading } from '../../common/loading';
import {
  create4Translations,
  translationsBeforeEach,
  visitTranslations,
} from '../../common/translations';

describe('Translations sorting', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach()
      .then((p) => (project = p))
      .then(() => createKey(project.id, 'zzz.first.created', {}))
      .then(() => create4Translations(project.id))
      .then(() => createKey(project.id, 'aaa.last.created', {}))
      .then(() => visitTranslations(project.id));
  });

  it('sort by key name a to z', () => {
    cy.gcy('translation-controls-order').click();
    cy.waitForDom();
    cy.gcy('translation-controls-order-item')
      .contains('Key name A to Z')
      .click();
    waitForGlobalLoading();
    cy.gcy('translations-key-name').eq(0).should('contain', 'aaa.last.created');
    cy.gcy('translations-key-name').eq(1).should('contain', 'Cool key 01');
  });

  it('sort by key name z to a', () => {
    cy.gcy('translation-controls-order').click();
    cy.waitForDom();
    cy.gcy('translation-controls-order-item')
      .contains('Key name Z to A')
      .click();

    cy.gcy('translations-key-name')
      .eq(0)
      .should('contain', 'zzz.first.created');
    cy.gcy('translations-key-name').eq(1).should('contain', 'Cool key 04');
  });

  it('sort from newest keys', () => {
    cy.gcy('translation-controls-order').click();
    cy.waitForDom();
    cy.gcy('translation-controls-order-item')
      .contains('First key added')
      .click();
    waitForGlobalLoading();
    cy.gcy('translations-key-name')
      .eq(0)
      .should('contain', 'zzz.first.created');
    cy.gcy('translations-key-name').eq(1).should('contain', 'Cool key 01');
  });

  it('sort from oldest keys', () => {
    cy.gcy('translation-controls-order').click();
    cy.waitForDom();
    cy.gcy('translation-controls-order-item')
      .contains('Last key added')
      .click();
    waitForGlobalLoading();
    cy.gcy('translations-key-name').eq(0).should('contain', 'aaa.last.created');
    cy.gcy('translations-key-name').eq(1).should('contain', 'Cool key 04');
  });
});
