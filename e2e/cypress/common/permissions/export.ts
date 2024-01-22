import { satisfiesLanguageAccess } from '../../../../webapp/src/fixtures/permissions';
import { dismissMenu } from '../shared';
import { getLanguageId, getLanguages, ProjectInfo } from './shared';

export function testExport({ project, languages }: ProjectInfo) {
  cy.gcy('export-language-selector').click();
  getLanguages().forEach(([tag, name]) => {
    if (
      satisfiesLanguageAccess(
        project.computedPermission,
        'translations.view',
        getLanguageId(languages, tag)
      )
    ) {
      cy.gcy('export-language-selector-item')
        .contains(name)
        .should('be.visible');
    } else {
      cy.gcy('export-language-selector-item')
        .contains(name)
        .should('not.exist');
    }
  });
  dismissMenu();
}
