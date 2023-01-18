import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { deleteProject } from '../../common/apiCalls/common';
import { visitProjectDashboard } from '../../common/languages';
import { waitForGlobalLoading } from '../../common/loading';

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

  it('outdated indicator logic', () => {
    getOutdatedIndicator('Studený přeložený text 1')
      .should('be.visible')
      .click();
    waitForGlobalLoading();
    getOutdatedIndicator('Studený přeložený text 1').should('exist');

    visitTranslations(project.id);
    getOutdatedIndicator('Studený přeložený text 1').should('not.exist');

    editCell('Cool translated text 1', 'Cool translated text 1 edited', true);
    waitForGlobalLoading();
    getOutdatedIndicator('Studený přeložený text 1').should('be.visible');
  });

  it('shows action in activity', () => {
    getOutdatedIndicator('Studený přeložený text 1')
      .should('be.visible')
      .click();
    visitProjectDashboard(project.id);

    const lastActivity = cy.gcy('activity-compact').first();
    lastActivity.contains('Updated state').should('be.visible');
  });

  const getOutdatedIndicator = (translationText: string) => {
    return getCell(translationText).findDcy(
      'translations-cell-outdated-button'
    );
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
