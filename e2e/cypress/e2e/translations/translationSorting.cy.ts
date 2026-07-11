import { createKey, login } from '../../common/apiCalls/common';
import { emptyProjectTestData } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import { visitTranslations } from '../../common/translations';

describe('Translations sorting', () => {
  let project: { name: string; id: number } = null;

  beforeEach(() => {
    emptyProjectTestData.clean();
    emptyProjectTestData
      .generateStandard()
      .then((data) => {
        project = data.body.projects[0];
        login('franta');
      })
      .then(() => createKey(project.id, 'z.first.created', {}))
      .then(() => createKey(project.id, 'b.second.created', {}))
      .then(() => createKey(project.id, 'c.third.created', {}))
      .then(() => createKey(project.id, 'a.last.created', {}))
      .then(() => visitTranslations(project.id));
  });

  afterEach(() => {
    emptyProjectTestData.clean();
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
      .contains('Newest keys on top')
      .click();
    waitForGlobalLoading();
    cy.gcy('translations-key-name').eq(0).should('contain', 'a.last.created');
    cy.gcy('translations-key-name').eq(1).should('contain', 'c.third.created');
  });

  it('sort from oldest keys', () => {
    cy.gcy('translation-controls-sort').click();
    cy.waitForDom();
    cy.gcy('translation-controls-sort-item')
      .contains('Oldest keys on top')
      .click();
    waitForGlobalLoading();
    cy.gcy('translations-key-name').eq(0).should('contain', 'z.first.created');
    cy.gcy('translations-key-name').eq(1).should('contain', 'b.second.created');
  });
});
