import {
  getProjectByNameFromTestData,
  qaTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';

describe('QA inline highlighting', () => {
  let projectId: number;
  let view: E2TranslationsView;

  beforeEach(() => {
    qaTestData.clean();
    qaTestData.generateStandard().then((res) => {
      projectId = getProjectByNameFromTestData(res.body, 'test_project')!.id;
    });
    login('test_username');
    view = new E2TranslationsView();
  });

  afterEach(() => {
    qaTestData.clean();
  });

  it('shows highlight on translations with positional QA issues', () => {
    view.visit(projectId);

    cy.contains('key_spacing_issue')
      .closestDcy('translations-row')
      .findDcy('qa-issue-highlight')
      .should('exist');
  });

  it('shows marker for zero-width QA issues', () => {
    view.visit(projectId);

    cy.contains('key_punctuation_issue')
      .closestDcy('translations-row')
      .findDcy('qa-issue-marker')
      .should('exist');
  });

  it('does not show highlights on translations without issues', () => {
    view.visit(projectId);

    cy.contains('key_no_issues')
      .closestDcy('translations-row')
      .findDcy('qa-issue-highlight')
      .should('not.exist');

    cy.contains('key_no_issues')
      .closestDcy('translations-row')
      .findDcy('qa-issue-marker')
      .should('not.exist');
  });
});
