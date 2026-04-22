import {
  getProjectByNameFromTestData,
  qaTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';
import { editTranslation } from '../../common/translations';

describe('QA cell badges', () => {
  let projectId: number;
  let disabledProjectId: number;

  let view: E2TranslationsView;

  beforeEach(() => {
    qaTestData.clean();
    qaTestData.generateStandard().then((res) => {
      projectId = getProjectByNameFromTestData(res.body, 'test_project')!.id;
      disabledProjectId = getProjectByNameFromTestData(
        res.body,
        'Disabled QA Project'
      )!.id;
    });
    login('test_username');
    view = new E2TranslationsView();
  });

  afterEach(() => {
    qaTestData.clean();
  });

  it('shows badge on translation cell with QA issues', () => {
    view.visit(projectId);

    cy.contains('key_placeholder_issue')
      .closestDcy('translations-row')
      .findDcy('translations-cell-qa-issues-button')
      .should('be.visible');
  });

  it('shows correct count for multiple issues', () => {
    view.visit(projectId);

    cy.contains('key_multiple_issues')
      .closestDcy('translations-row')
      .findDcy('translations-cell-qa-issues-button')
      .should('be.visible')
      .should('contain.text', '4');
  });

  it('does not show badge for translation without issues', () => {
    view.visit(projectId);

    cy.contains('key_no_issues')
      .closestDcy('translations-row')
      .findDcy('translations-cell-qa-issues-button')
      .should('not.exist');
  });

  it('does not show badge for translation with only ignored issues', () => {
    view.visit(projectId);

    cy.contains('key_ignored_issue')
      .closestDcy('translations-row')
      .findDcy('translations-cell-qa-issues-button')
      .should('not.exist');
  });

  it('does not show badge after editing translation in QA-disabled project', () => {
    view.visit(disabledProjectId);

    editTranslation({
      key: 'disabled_key',
      languageTag: 'fr',
      newValue: 'Edited',
    });

    cy.contains('disabled_key')
      .closestDcy('translations-row')
      .findDcy('translations-cell-qa-issues-button')
      .should('not.exist');
  });

  it('clicking badge opens QA panel', () => {
    view.visit(projectId);

    cy.contains('key_placeholder_issue')
      .closestDcy('translations-row')
      .findDcy('translations-cell-qa-issues-button')
      .click();

    gcy('qa-panel-container').should('exist');
    gcy('qa-check-item').should('have.length', 1);
  });
});
