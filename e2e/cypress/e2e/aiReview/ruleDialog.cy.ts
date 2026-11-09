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

  it('creates a rule with tags, without tags, languages, branches and a custom threshold', () => {
    const dialog = openCreateDialog(branchingProjectId);
    gcy('ai-review-rule-form-name').type('Branch rule');

    gcy('ai-review-rule-form-keys-filter-toggle').click();
    gcy('ai-review-rule-form-tag-in').type('ui{enter}');
    gcy('ai-review-rule-form-tag-in').click();
    dialog.getScopeOption('').click();
    gcy('ai-review-rule-form-name').click();
    gcy('ai-review-rule-form-tag-not-in').type('legal{enter}');

    gcy('ai-review-rule-form-languages')
      .findDcy('translations-language-select-form-control')
      .click();
    gcyAdvanced({
      value: 'translations-language-select-item',
      tag: 'de',
    }).click();
    cy.get('body').type('{esc}');

    gcy('ai-review-rule-form-branch-in').type('release{enter}');
    gcy('ai-review-rule-form-branch-not-in').type('feature/*{enter}');

    dialog.pickThresholdPreset('custom').setThresholdSlider(90);
    gcyAdvanced({
      value: 'ai-review-rule-form-threshold-preset',
      option: 'custom',
    }).should('contain', '90+');

    dialog.save();
    waitForGlobalLoading();

    gcyAdvanced({ value: 'ai-review-rule-item', name: 'Branch rule' }).within(
      () => {
        gcyAdvanced({
          value: 'ai-review-scope-chip',
          scope: 'ui',
          kind: 'include',
        }).should('exist');
        gcyAdvanced({
          value: 'ai-review-scope-chip',
          scope: 'legal',
          kind: 'exclude',
        }).should('exist');
        gcyAdvanced({
          value: 'ai-review-scope-chip',
          scope: '',
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
        gcyAdvanced({
          value: 'ai-review-scope-chip',
          scope: 'languages',
        }).should('have.attr', 'data-cy-value', 'German');
      }
    );

    rulesView.openEditDialog('Branch rule');
    gcyAdvanced({
      value: 'ai-review-rule-form-threshold-preset',
      option: 'custom',
    })
      .should('have.attr', 'data-cy-selected', 'true')
      .should('contain', '90+');
  });

  it('selecting a branch removes it from the except options', () => {
    const dialog = openCreateDialog(branchingProjectId);
    gcy('ai-review-rule-form-keys-filter-toggle').click();
    gcy('ai-review-rule-form-branch-in').type('main{enter}');
    gcy('ai-review-rule-form-branch-not-in').click();
    dialog.getScopeOption('release').should('be.visible');
    dialog.getScopeOption('main').should('not.exist');
  });

  it('a threshold preset selects its value and Custom shows the slider', () => {
    const dialog = openCreateDialog(projectId);
    gcyAdvanced({
      value: 'ai-review-rule-form-threshold-preset',
      option: '85',
    }).should('have.attr', 'data-cy-selected', 'true');
    gcy('ai-review-rule-form-threshold-slider').should('not.exist');
    dialog.pickThresholdPreset('95');
    gcy('ai-review-rule-form-threshold-hint').should('contain', '95');
    dialog.pickThresholdPreset('custom');
    gcy('ai-review-rule-form-threshold-slider').should('be.visible');
    gcyAdvanced({
      value: 'ai-review-rule-form-threshold-preset',
      option: 'custom',
    }).should('contain', '95+');
  });

  it('Never hides threshold and mode and disables prompt and QA checkbox', () => {
    openCreateDialog(branchingProjectId);
    gcy('ai-review-rule-form-threshold-presets').should('be.visible');
    gcy('ai-review-rule-form-mode').should('be.visible');
    gcy('ai-review-rule-form-never').click();
    gcy('ai-review-rule-form-threshold-presets').should('not.exist');
    gcy('ai-review-rule-form-mode').should('not.exist');
    gcy('ai-review-rule-form-advanced-toggle').click();
    gcy('ai-review-rule-form-prompt').should(
      'have.attr',
      'aria-disabled',
      'true'
    );
    gcy('ai-review-rule-form-skip-qa').find('input').should('be.disabled');
  });

  it('adding a threshold override to a Never rule brings back mode and enables prompt and QA', () => {
    openCreateDialog(branchingProjectId);
    gcy('ai-review-rule-form-never').click();
    gcy('ai-review-rule-form-advanced-toggle').click();
    gcy('ai-review-rule-form-override-add').click();
    gcy('ai-review-rule-form-override-instead')
      .scrollIntoView()
      .should('be.visible');
    gcy('ai-review-rule-form-override-hint')
      .scrollIntoView()
      .should('be.visible');
    gcy('ai-review-rule-form-mode').should('be.visible');
    gcy('ai-review-rule-form-prompt').should(
      'not.have.attr',
      'aria-disabled',
      'true'
    );
    gcy('ai-review-rule-form-skip-qa').find('input').should('not.be.disabled');
    gcyAdvanced({
      value: 'ai-review-rule-form-threshold-preset',
      option: '85',
    }).should('be.disabled');
  });

  it('Never in an override row disables its threshold field', () => {
    openCreateDialog(projectId);
    gcy('ai-review-rule-form-advanced-toggle').click();
    gcy('ai-review-rule-form-override-add').click();
    gcy('ai-review-rule-form-override-threshold')
      .find('input')
      .should('not.be.disabled');
    gcy('ai-review-rule-form-override-never').click();
    gcy('ai-review-rule-form-override-threshold')
      .find('input')
      .should('be.disabled');
  });

  it('adds and removes a language override', () => {
    openCreateDialog(projectId);
    gcy('ai-review-rule-form-advanced-toggle').click();
    gcy('ai-review-rule-form-override-add').click();
    gcy('ai-review-rule-form-override-row').should('have.length', 1);
    gcy('ai-review-rule-form-override-remove').click();
    gcy('ai-review-rule-form-override-row').should('not.exist');
  });

  it('branch fields are hidden when branching is off', () => {
    openCreateDialog(projectId);
    gcy('ai-review-rule-form-keys-filter-toggle').click();
    gcy('ai-review-rule-form-tag-in').should('exist');
    gcy('ai-review-rule-form-branch-in').should('not.exist');
    gcy('ai-review-rule-form-branch-not-in').should('not.exist');
  });

  it('QA checkbox is hidden when QA is off', () => {
    openCreateDialog(projectId);
    gcy('ai-review-rule-form-advanced-toggle').click();
    gcy('ai-review-rule-form-prompt').should('be.visible');
    gcy('ai-review-rule-form-skip-qa').should('not.exist');
  });

  it('edit keeps values', () => {
    rulesView.visit(projectId);
    const dialog = rulesView.openEditDialog('UI rule');
    gcy('ai-review-rule-form-name')
      .find('input')
      .should('have.value', 'UI rule');
    gcyAdvanced({
      value: 'ai-review-rule-form-threshold-preset',
      option: '85',
    }).should('have.attr', 'data-cy-selected', 'true');
    dialog.save();
    waitForGlobalLoading();
    gcyAdvanced({ value: 'ai-review-rule-item', name: 'UI rule' }).should(
      'exist'
    );
  });
});
