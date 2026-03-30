import {
  getProjectByNameFromTestData,
  qaTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';

describe('QA cell badges', () => {
  let projectId: number;

  beforeEach(() => {
    qaTestData.clean();
    qaTestData.generateStandard().then((res) => {
      projectId = getProjectByNameFromTestData(res.body, 'test_project')!.id;
    });
  });

  afterEach(() => {
    qaTestData.clean();
  });

  it('shows badge on translation cell with QA issues', () => {
    login('test_username');
    const view = new E2TranslationsView();
    view.visit(projectId);

    cy.contains('key_placeholder_issue')
      .closestDcy('translations-row')
      .findDcy('translations-cell-qa-issues-button')
      .should('be.visible');
  });

  it('shows correct count for multiple issues', () => {
    login('test_username');
    const view = new E2TranslationsView();
    view.visit(projectId);

    cy.contains('key_multiple_issues')
      .closestDcy('translations-row')
      .findDcy('translations-cell-qa-issues-button')
      .should('be.visible')
      .should('contain.text', '4');
  });

  it('does not show badge for translation without issues', () => {
    login('test_username');
    const view = new E2TranslationsView();
    view.visit(projectId);

    cy.contains('key_no_issues')
      .closestDcy('translations-row')
      .findDcy('translations-cell-qa-issues-button')
      .should('not.exist');
  });

  it('does not show badge for translation with only ignored issues', () => {
    login('test_username');
    const view = new E2TranslationsView();
    view.visit(projectId);

    cy.contains('key_ignored_issue')
      .closestDcy('translations-row')
      .findDcy('translations-cell-qa-issues-button')
      .should('not.exist');
  });

  it('clicking badge opens QA panel', () => {
    login('test_username');
    const view = new E2TranslationsView();
    view.visit(projectId);

    cy.contains('key_placeholder_issue')
      .closestDcy('translations-row')
      .findDcy('translations-cell-qa-issues-button')
      .click();

    gcy('qa-panel-container').should('exist');
    gcy('qa-check-item').should('have.length', 1);
  });
});
