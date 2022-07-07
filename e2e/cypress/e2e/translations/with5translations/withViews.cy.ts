import { getAnyContainingText } from '../../../common/xPath';
import { ProjectDTO } from '../../../../../webapp/src/service/response.types';
import {
  confirmSaveChanges,
  create4Translations,
  editCell,
  forEachView,
  getCellCancelButton,
  translationsBeforeEach,
  visitTranslations,
} from '../../../common/translations';

describe('Views with 5 Translations', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach()
      .then((p) => (project = p))
      .then(() => create4Translations(project.id))
      .then(() => {
        visitTranslations(project.id);
      });
  });

  // run same tests for list view and table view
  forEachView(
    () => project.id,
    () => {
      it('will edit key', () => {
        editCell('Cool key 01', 'Cool key edited');

        cy.contains('Cool key edited').should('be.visible');
        cy.contains('Cool key 02').should('be.visible');
        cy.contains('Cool key 04').should('be.visible');
      });

      it('will edit translation', () => {
        editCell('Cool translated text 1', 'Super cool changed text...');
        cy.xpath(
          `${getAnyContainingText(
            'Super cool changed text...'
          )}/parent::*//button[@type='submit']`
        ).should('not.exist');
        cy.contains('Super cool changed text...').should('be.visible');
        cy.contains('Cool translated text 2').should('be.visible');
      });

      it('will cancel key edit without confirmation', () => {
        editCell('Cool key 01', 'Cool key edited', false);
        getCellCancelButton().click();

        cy.contains('Cool key edited').should('not.exist');
        cy.contains('Cool key 01').should('be.visible');
      });

      it('will ask for confirmation on changed edit', () => {
        editCell('Cool key 01', 'Cool key edited', false);
        cy.contains('Cool key 04').click();
        cy.contains(`Unsaved changes`).should('be.visible');
        confirmSaveChanges();
        cy.contains('Cool key edited');
        cy.gcy('global-editor').contains('Cool key edited').should('not.exist');
        cy.gcy('global-editor').contains('Cool key 04').should('be.visible');
      });
    }
  );
});
