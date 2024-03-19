import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { deleteProject } from '../../common/apiCalls/common';
import { waitForGlobalLoading } from '../../common/loading';
import { assertMessage, visitProjectDashboard } from '../../common/shared';

import {
  create4Translations,
  editCell,
  selectLangsInLocalstorage,
  translationsBeforeEach,
  visitTranslations,
} from '../../common/translations';

describe('Translation states', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach()
      .then((p) => (project = p))
      .then(() => {
        create4Translations(project.id);
        selectLangsInLocalstorage(project.id, ['en', 'cs']);
        visit();
      });
  });

  afterEach(() => {
    deleteProject(project.id);
  });

  it('outdated indicator logic', { retries: { runMode: 3 } }, () => {
    getOutdatedIndicator('Studený přeložený text 1').should('not.exist');
    editCell('Cool translated text 1', 'Cool translated text 1 edited', true);

    getOutdatedIndicator('Studený přeložený text 1').should('be.visible');
    getRemoveOutdatedIndicator('Studený přeložený text 1').click();
    waitForGlobalLoading();
    getOutdatedIndicator('Studený přeložený text 1').should('not.exist');
  });

  it('shows action in activity', () => {
    getOutdatedIndicator('Studený přeložený text 1').should('not.exist');
    editCell('Cool translated text 1', 'Cool translated text 1 edited', true);

    getOutdatedIndicator('Studený přeložený text 1').should('be.visible');
    getRemoveOutdatedIndicator('Studený přeložený text 1').click();
    visitProjectDashboard(project.id);

    const lastActivity = cy.gcy('activity-compact').first();
    lastActivity.contains('Updated state').should('be.visible');
  });

  it("won't mark empty translation", () => {
    editCell('Studený přeložený text 1', '{del}', true);
    waitForGlobalLoading();

    editCell('Cool translated text 1', 'Cool translated text 1 edited', true);
    waitForGlobalLoading();
    cy.gcy('translations-table-cell')
      .eq(2)
      .findDcy('translations-outdated-indicator')
      .should('not.exist');
  });

  it('gets resolved on edit', () => {
    getOutdatedIndicator('Studený přeložený text 1').should('not.exist');
    editCell('Cool translated text 1', 'Cool translated text 1 edited', true);

    editCell(
      'Studený přeložený text 1',
      'Studený přeložený text 1 edited',
      true
    );
    waitForGlobalLoading();

    getOutdatedIndicator('Studený přeložený text 1 edited').should('not.exist');
  });

  it('gets resolved on state change', () => {
    getOutdatedIndicator('Studený přeložený text 1').should('not.exist');
    editCell('Cool translated text 1', 'Cool translated text 1 edited', true);

    getCell('Studený přeložený text 1')
      .trigger('mouseover')
      .findDcy('translation-state-button')
      .click();
    waitForGlobalLoading();

    getOutdatedIndicator('Studený přeložený text 1').should('not.exist');
  });

  it('wont appear when adding new key', () => {
    cy.gcy('translations-add-button').click();
    cy.gcy('translation-create-key-input').type('test_key');
    cy.gcy('translation-create-translation-input')
      .find('[contenteditable]')
      .type('Test translation');

    cy.gcy('global-form-save-button').click();
    assertMessage('Key created');

    cy.gcy('translations-outdated-indicator').should('not.exist');
  });

  const getOutdatedIndicator = (translationText: string) => {
    return getCell(translationText).findDcy('translations-outdated-indicator');
  };

  const getRemoveOutdatedIndicator = (translationText: string) => {
    return getCell(translationText)
      .findDcy('translations-outdated-clear-button')
      .invoke('show');
  };

  const getCell = (translationText: string) => {
    return cy
      .contains(translationText)
      .should('be.visible')
      .closestDcy('translations-table-cell');
  };

  const visit = () => {
    visitTranslations(project.id);
  };
});
