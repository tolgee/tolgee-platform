import { translationsDisabled } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { visitTranslations } from '../../common/translations';
import { gcyAdvanced } from '../../common/shared';

describe('Translation disabled', () => {
  beforeEach(() => {
    translationsDisabled.clean();
    translationsDisabled
      .generateStandard()
      .then((r) => r.body)
      .then(({ users, projects }) => {
        login(users[0].username);
        const testProject = projects[0];
        visitTranslations(testProject.id);
      });
  });

  it('german translation is disabled', () => {
    gcyAdvanced({
      value: 'translations-table-cell-translation',
      lang: 'de',
    })
      .contains('<disabled>')
      .should('be.visible')
      .click();
    cy.gcy('global-editor').should('not.exist');
  });

  it('english translation is not disabled', () => {
    gcyAdvanced({ value: 'translations-table-cell-translation', lang: 'en' })
      .contains('What a text')
      .should('be.visible')
      .click();
    cy.gcy('global-editor').should('exist');
  });

  it('disables english', () => {
    cy.gcy('translations-table-cell').contains('key').click();
    cy.gcy('key-edit-tab-advanced').click();
    cy.gcy('permissions-language-menu-button').click();
    cy.gcy('search-select-item').contains('English').click();
    cy.focused().type('{esc}');
    cy.gcy('translations-cell-main-action-button').click();

    gcyAdvanced({ value: 'translations-table-cell-translation', lang: 'en' })
      .contains('<disabled>')
      .should('be.visible')
      .click();
    cy.gcy('global-editor').should('not.exist');
  });
});
