import {
  aiReviewTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy, gcyAdvanced } from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';
import { E2AiReviewRulesView } from '../../compounds/aiReview/E2AiReviewRulesView';

describe('AI review rule dialog', () => {
  let projectId: number;
  let branchingProjectId: number;
  const rulesView = new E2AiReviewRulesView();

  beforeEach(() => {
    aiReviewTestData.clean();
    aiReviewTestData.generateStandard().then((res) => {
      projectId = getProjectByNameFromTestData(
        res.body,
        'ai_review_project'
      )!.id;
      branchingProjectId = getProjectByNameFromTestData(
        res.body,
        'ai_review_branching_project'
      )!.id;
    });
    login('test_username');
  });

  afterEach(() => {
    aiReviewTestData.clean();
  });

  function openCreateDialog(id: number) {
    rulesView.visit(id);
    return rulesView.openCreateDialog();
  }

  it('creates a rule with tags, languages, branches and a custom threshold', () => {
    const dialog = openCreateDialog(branchingProjectId);
    dialog.getNameInput().type('Branch rule');

    gcy('ai-review-rule-form-keys-filter-select').click();
    gcyAdvanced({ value: 'submenu-item', id: 'tags' }).click();
    gcy('filter-item').contains('ui').click();
    cy.get('body').type('{esc}');
    cy.get('body').type('{esc}');

    gcy('ai-review-rule-form-languages-select').click();
    gcyAdvanced({
      value: 'translations-language-select-item',
      tag: 'de',
    }).click();
    cy.get('body').type('{esc}');

    gcy('ai-review-rule-form-branch-in').type('release{enter}');
    gcy('ai-review-rule-form-branch-not-in').type('feature/*{enter}');

    dialog.selectActiveMode();
    dialog.setThreshold(90);

    dialog.save();
    waitForGlobalLoading();

    rulesView.getRule('Branch rule').within(() => {
      gcy('ai-review-rule-language-flag').should('have.length', 1);
      // Two or more filters are shown as a count; the tooltip lists them.
      gcy('ai-review-rule-filters-count')
        .should('contain', '3 filters')
        .trigger('mouseover');
    });
    gcy('ai-review-rule-filters-tooltip').within(() => {
      gcyAdvanced({
        value: 'ai-review-scope-chip',
        scope: 'ui',
        kind: 'include',
      }).should('exist');
      gcyAdvanced({
        value: 'ai-review-scope-chip',
        scope: 'release',
        kind: 'include',
      }).should('exist');
      gcyAdvanced({
        value: 'ai-review-scope-chip',
        scope: 'feature/*',
        kind: 'exclude',
      }).should('exist');
    });

    rulesView
      .openEditDialog('Branch rule')
      .getThresholdInput()
      .should('have.value', '90');
  });

  it('selecting a branch removes it from the except options', () => {
    const dialog = openCreateDialog(branchingProjectId);
    gcy('ai-review-rule-form-branch-in').type('main{enter}');
    gcy('ai-review-rule-form-branch-not-in').click();
    dialog.getScopeOption('release').should('be.visible');
    dialog.getScopeOption('main').should('not.exist');
  });

  it('the threshold shows only for Active rules', () => {
    const dialog = openCreateDialog(projectId);
    gcy('ai-review-rule-form-threshold-slider').should('not.exist');
    dialog.selectActiveMode();
    gcy('ai-review-rule-form-threshold-slider').should('be.visible');
    dialog.selectScoreOnlyMode();
    gcy('ai-review-rule-form-threshold-slider').should('not.exist');
  });

  it('Never hides prompt, mode, threshold and QA and shows the info', () => {
    openCreateDialog(branchingProjectId);
    gcy('ai-review-rule-form-mode').should('be.visible');
    gcy('ai-review-rule-form-never').click();
    gcy('ai-review-rule-form-never-info').should('be.visible');
    gcy('ai-review-rule-form-mode').should('not.exist');
    gcy('ai-review-rule-form-prompt').should('not.exist');
    gcy('ai-review-rule-form-threshold-slider').should('not.exist');
    gcy('ai-review-rule-form-skip-qa').should('not.exist');
  });

  it('Save needs a name and at least one language', () => {
    const dialog = openCreateDialog(projectId);
    dialog.getSaveButton().should('be.disabled');
    dialog.getNameInput().type('Named');
    dialog.getSaveButton().should('not.be.disabled');
    gcy('ai-review-rule-form-languages-select').click();
    gcy('ai-review-rule-form-languages-all').click();
    cy.get('body').type('{esc}');
    dialog.getSaveButton().should('be.disabled');
  });

  it('branch fields are hidden when branching is off', () => {
    openCreateDialog(projectId);
    gcy('ai-review-rule-form-keys-filter-select').should('exist');
    gcy('ai-review-rule-form-branch-in').should('not.exist');
    gcy('ai-review-rule-form-branch-not-in').should('not.exist');
  });

  it('QA checkbox is hidden when QA is off', () => {
    openCreateDialog(projectId);
    gcy('ai-review-rule-form-prompt').should('be.visible');
    gcy('ai-review-rule-form-skip-qa').should('not.exist');
  });

  it('edit keeps values', () => {
    rulesView.visit(projectId);
    const dialog = rulesView.openEditDialog('UI rule');
    dialog.getNameInput().should('have.value', 'UI rule');
    gcy('ai-review-rule-form-mode-active').find('input').should('be.checked');
    dialog.getThresholdInput().should('have.value', '85');
    dialog.save();
    waitForGlobalLoading();
    rulesView.getRule('UI rule').should('exist');
  });
});
