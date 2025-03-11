import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { createKey } from '../../common/apiCalls/common';
import { waitForGlobalLoading } from '../../common/loading';
import {
  translationsBeforeEach,
  visitTranslations,
} from '../../common/translations';

describe('Translations sorting', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach()
      .then((p) => (project = p))
      .then(() => createKey(project.id, 'z.first.created', {}))
      .then(() => createKey(project.id, 'b.second.created', {}))
      .then(() => createKey(project.id, 'c.third.created', {}))
      .then(() => createKey(project.id, 'a.last.created', {}))
      .then(() => visitTranslations(project.id));
  });

  it('sort by key name a to z', () => {
    cy.gcy('translation-controls-sort').click();
    cy.waitForDom();
    cy.gcy('translation-controls-sort-item')
      .contains('Key name A to Z')
      .click();
    waitForGlobalLoading();
    cy.gcy('translations-key-name').eq(0).should('contain', 'a.last.created');
    cy.gcy('translations-key-name').eq(1).should('contain', 'b.second.created');
  });

  it('sort by key name z to a', () => {
    cy.gcy('translation-controls-sort').click();
    cy.waitForDom();
    cy.gcy('translation-controls-sort-item')
      .contains('Key name Z to A')
      .click();

    cy.gcy('translations-key-name').eq(0).should('contain', 'z.first.created');
    cy.gcy('translations-key-name').eq(1).should('contain', 'c.third.created');
  });

  it('sort from newest keys', () => {
    cy.gcy('translation-controls-sort').click();
    cy.waitForDom();
    cy.gcy('translation-controls-sort-item')
      .contains('First key added')
      .click();
    waitForGlobalLoading();
    cy.gcy('translations-key-name').eq(0).should('contain', 'z.first.created');
    cy.gcy('translations-key-name').eq(1).should('contain', 'b.second.created');
  });

  it('sort from oldest keys', () => {
    cy.gcy('translation-controls-sort').click();
    cy.waitForDom();
    cy.gcy('translation-controls-sort-item').contains('Last key added').click();
    waitForGlobalLoading();
    cy.gcy('translations-key-name').eq(0).should('contain', 'a.last.created');
    cy.gcy('translations-key-name').eq(1).should('contain', 'c.third.created');
  });
});
