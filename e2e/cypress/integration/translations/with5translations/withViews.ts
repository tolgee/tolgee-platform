import { getAnyContainingText } from '../../../common/xPath';
import { ProjectDTO } from '../../../../../webapp/src/service/response.types';
import {
  clickDiscardChanges,
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

      it('will cancel key edit', () => {
        editCell('Cool key 01', 'Cool key edited', false);
        getCellCancelButton().click();

        cy.contains('Discard changes?').should('be.visible');
        clickDiscardChanges();
        cy.contains('Cool key edited').should('not.exist');
        cy.contains('Cool key 01').should('be.visible');
      });

      it('will ask for confirmation on changed edit', () => {
        editCell('Cool key 01', 'Cool key edited', false);
        cy.contains('Cool key 04').click();
        cy.contains(`Discard changes?`).should('be.visible');
        clickDiscardChanges();
        cy.contains('Cool key 04')
          .xpath(
            "./ancestor::*[@data-cy='translations-table-cell']//*[@data-cy='translations-cell-save-button']"
          )
          .should('be.visible');
      });
    }
  );
});
