import {
  satisfiesLanguageAccess,
  ScopeWithLanguage,
} from '../../../../webapp/src/fixtures/permissions';
import { commentsButton, deleteComment, resolveComment } from '../comments';
import { waitForGlobalLoading } from '../loading';
import { getCell } from '../state';
import {
  getLanguageId,
  pageAcessibleWithoutErrors,
  ProjectInfo,
} from './shared';

function getRowTexts(row: number) {
  return [
    ['en', `English text ${row}`],
    ['de', `German text ${row}`],
    ['cs', `Czech text ${row}`],
  ];
}

export function testTranslations({ project, languages }: ProjectInfo) {
  const scopes = project.computedPermission.scopes;

  function languageAccess(scope: ScopeWithLanguage, tag: string) {
    return satisfiesLanguageAccess(
      project.computedPermission,
      scope,
      getLanguageId(languages, tag)
    );
  }

  if (scopes.includes('translations.view')) {
    getRowTexts(1).forEach(([lang, text]) => {
      if (languageAccess('translations.view', lang)) {
        cy.gcy('translations-table-cell-translation')
          .contains(text)
          .should('be.visible');
      } else {
        cy.gcy('translations-table-cell-translation')
          .contains(text)
          .should('not.exist');
      }
    });
  }

  if (scopes.includes('translations.edit')) {
    getRowTexts(1).forEach(([lang, text]) => {
      if (languageAccess('translations.edit', lang)) {
        cy.gcy('translations-table-cell-translation').contains(text).click();
        cy.gcy('global-editor').should('be.visible');

        if (project.baseLanguage.tag !== lang) {
          cy.gcy('translation-tools-machine-translation-item').should(
            'be.visible'
          );
          cy.gcy('translation-tools-translation-memory-item').should(
            'be.visible'
          );
        }
        cy.gcy('translations-cell-cancel-button').click();
      } else if (languageAccess('translations.view', lang)) {
        cy.gcy('translations-table-cell-translation').contains(text).click();
        cy.gcy('global-editor').should('not.exist');
      }
    });
  }

  if (scopes.includes('translations.state-edit')) {
    getRowTexts(1).forEach(([lang, text]) => {
      if (languageAccess('translations.state-edit', lang)) {
        getCell(text).trigger('mouseover');
        getCell(text)
          .findDcy('translation-state-button')
          .should('exist')
          .click();
        pageAcessibleWithoutErrors();
      } else if (languageAccess('translations.view', lang)) {
        getCell(text).trigger('mouseover');
        getCell(text).findDcy('translation-state-button').should('not.exist');
      }
    });
  }

  getRowTexts(1).forEach(([lang, text]) => {
    if (languageAccess('translations.view', lang)) {
      cy.gcy('translations-state-indicator').should('be.visible');

      commentsButton('key-10', lang).click();
      cy.gcy('comment-text').should('be.visible');

      if (scopes.includes('translation-comments.set-state')) {
        resolveComment('comment 1');
      }

      if (scopes.includes('translation-comments.edit')) {
        deleteComment('comment 1');
      }

      if (scopes.includes('translation-comments.add')) {
        cy.gcy('translations-comments-input')
          .type('test comment')
          .type('{enter}');
        waitForGlobalLoading();
        cy.gcy('comment-text').contains('test comment').should('be.visible');
      }

      cy.gcy('translations-cell-cancel-button').click();
    }
  });
}
