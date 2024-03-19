import 'cypress-file-upload';
import { gcy } from '../../common/shared';
import {
  findResolutionRow,
  getLanguageRow,
  visitImport,
} from '../../common/import';
import { importTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';

describe('Import Resolving', () => {
  describe('with basic data', () => {
    beforeEach(() => {
      importTestData.clean();
      importTestData.generateBasic().then((importData) => {
        login('franta');
        visitImport(importData.body.project.id);
      });
    });

    it('shows correct initial data', () => {
      getLanguageRow('multilang.json (en)')
        .findDcy('import-result-resolve-button')
        .click();
      gcy('import-resolution-dialog-resolved-count').should('have.text', '0');
      gcy('import-resolution-dialog-conflict-count').should('have.text', '3');
      gcy('import-resolution-dialog-show-resolved-switch')
        .find('input')
        .should('be.checked');
      gcy('import-resolution-dialog-data-row').should('have.length', 3);
      gcy('import-resolution-dialog-data-row').should(
        'contain.text',
        'What a text'
      );
    });

    it('resolves row (one by one)', () => {
      getLanguageRow('multilang.json (en)')
        .findDcy('import-result-resolve-button')
        .click();
      gcy('import-resolution-dialog-data-row').contains('Overridden').click();
      cy.xpath('//*[@data-cy-selected]').should('have.length', 1);
      findResolutionRow('what a key')
        .findDcy('import-resolution-dialog-existing-translation')
        .should('not.have.attr', 'data-cy-selected');
      findResolutionRow('what a key')
        .findDcy('import-resolution-dialog-new-translation')
        .should('have.attr', 'data-cy-selected');

      findResolutionRow('what a nice key').contains('What a text').click();
      cy.xpath('//*[@data-cy-selected]').should('have.length', 2);
      findResolutionRow('what a nice key')
        .findDcy('import-resolution-dialog-new-translation')
        .should('not.have.attr', 'data-cy-selected');
      findResolutionRow('what a nice key')
        .findDcy('import-resolution-dialog-existing-translation')
        .should('have.attr', 'data-cy-selected');

      gcy('import-resolution-dialog-resolved-count').should('have.text', '2');
    });

    it('accept all new', () => {
      getLanguageRow('multilang.json (en)')
        .findDcy('import-result-resolve-button')
        .click();
      gcy('import-resolution-dialog-accept-imported-button').click();
      cy.xpath('//*[@data-cy-selected]').should('have.length', 3);
      gcy('import-resolution-dialog-new-translation').each(($el) => {
        cy.wrap($el).should('have.attr', 'data-cy-selected');
      });
      gcy('import-resolution-dialog-resolved-count').should('have.text', '3');
    });

    it('accept all old', () => {
      getLanguageRow('multilang.json (en)')
        .findDcy('import-result-resolve-button')
        .click();
      gcy('import-resolution-dialog-accept-old-button').click();
      cy.xpath('//*[@data-cy-selected]').should('have.length', 3);
      gcy('import-resolution-dialog-existing-translation').each(($el) => {
        cy.wrap($el).should('have.attr', 'data-cy-selected');
      });
      gcy('import-resolution-dialog-resolved-count').should('have.text', '3');
    });
  });

  describe('with long text', () => {
    beforeEach(() => {
      importTestData.clean();
      importTestData.generateWithLongText().then((importData) => {
        login('franta');
        visitImport(importData.body.project.id);
      });
    });

    it('overflow ellipsis and logs text expansion', () => {
      getLanguageRow('multilang.json (en)')
        .findDcy('import-result-resolve-button')
        .click();

      const assertBothExpanded = () => {
        cy.contains('Hello, I am old translation')
          .closestDcy('import-resolution-dialog-existing-translation')
          .then(($text: any) => {
            cy.wrap($text!.height()).should('be.greaterThan', 80);
          });
        cy.contains('Hello, I am translation')
          .closestDcy('import-resolution-dialog-new-translation')
          .then(($text: any) => {
            cy.wrap($text!.height()).should('be.greaterThan', 80);
          });
      };

      const assertBothCollapsed = () => {
        cy.contains('Hello, I am old translation')
          .closestDcy('import-resolution-dialog-existing-translation')
          .then(($text: any) => {
            cy.wrap($text!.width()).should('be.lessThan', 520);
            cy.wrap($text!.height()).should('be.lessThan', 70);
          });
        cy.contains('Hello, I am translation')
          .closestDcy('import-resolution-dialog-new-translation')
          .then(($text: any) => {
            cy.wrap($text!.width()).should('be.lessThan', 520);
            cy.wrap($text!.height()).should('be.lessThan', 70);
          });
      };

      assertBothCollapsed();

      [0, 1].forEach((i) => {
        gcy('import-resolution-translation-expand-button').eq(i).click();
        assertBothExpanded();
        gcy('import-resolution-translation-expand-button').eq(i).click();
        assertBothCollapsed();
      });
    });
  });

  after(() => {
    importTestData.clean();
  });
});
