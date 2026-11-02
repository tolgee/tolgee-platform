import {
  aiReviewTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy, gcyAdvanced } from '../../common/shared';
import { visitAiReviewRules } from '../../common/aiReview';
import { waitForGlobalLoading } from '../../common/loading';

describe('AI review rule dialog', () => {
  let projectId: number;
  let branchingProjectId: number;

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
    visitAiReviewRules(id);
    gcy('ai-review-add-rule-button').click();
    gcy('ai-review-rule-dialog').should('be.visible');
  }

  it('creates a rule with tags, without tags, languages, branches and threshold', () => {
    openCreateDialog(branchingProjectId);
    gcy('ai-review-rule-form-name').type('Branch rule');

    gcy('ai-review-rule-form-keys-select').click();
    gcyAdvanced({ value: 'submenu-item', id: 'tags' }).click();
    cy.gcy('filter-item').contains('ui').click();
    cy.gcy('filter-item')
      .contains('legal')
      .closestDcy('filter-item')
      .findDcy('filter-item-exclude')
      .click({ force: true });
    gcyAdvanced({ value: 'filter-item', id: 'without-tags' }).click();
    cy.get('body').type('{esc}');
    cy.get('body').type('{esc}');

    gcy('ai-review-rule-form-languages')
      .findDcy('translations-language-select-form-control')
      .click();
    gcy('translations-language-select-item').contains('German').click();
    cy.get('body').type('{esc}');

    gcy('ai-review-rule-form-branch-in').type('release{enter}');
    gcy('ai-review-rule-form-branch-not-in').type('feature/*{enter}');

    gcy('ai-review-rule-form-threshold').find('input').clear().type('90');

    gcy('ai-review-rule-form-save').click();
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

    gcyAdvanced({ value: 'ai-review-rule-edit', name: 'Branch rule' }).click();
    gcy('ai-review-rule-form-threshold')
      .find('input')
      .should('have.value', '90');
  });

  it('Never checkbox disables threshold, mode, prompt and QA checkbox', () => {
    openCreateDialog(branchingProjectId);
    gcy('ai-review-rule-form-threshold')
      .find('input')
      .should('not.be.disabled');
    gcy('ai-review-rule-form-never').click();
    gcy('ai-review-rule-form-threshold').find('input').should('be.disabled');
    gcy('ai-review-rule-form-mode').should(
      'have.attr',
      'aria-disabled',
      'true'
    );
    gcy('ai-review-rule-form-prompt').should(
      'have.attr',
      'aria-disabled',
      'true'
    );
    gcy('ai-review-rule-form-skip-qa').find('input').should('be.disabled');
  });

  it('adding a threshold override to a Never rule enables mode, prompt and QA checkbox with the hint', () => {
    openCreateDialog(branchingProjectId);
    gcy('ai-review-rule-form-never').click();
    gcy('ai-review-rule-form-overrides-toggle').click();
    gcy('ai-review-rule-form-override-add').click();
    gcy('ai-review-rule-form-override-instead').should('be.visible');
    gcy('ai-review-rule-form-override-hint').should('be.visible');
    gcy('ai-review-rule-form-mode').should(
      'not.have.attr',
      'aria-disabled',
      'true'
    );
    gcy('ai-review-rule-form-prompt').should(
      'not.have.attr',
      'aria-disabled',
      'true'
    );
    gcy('ai-review-rule-form-skip-qa').find('input').should('not.be.disabled');
    gcy('ai-review-rule-form-threshold').find('input').should('be.disabled');
  });

  it('Never in an override row disables its threshold field', () => {
    openCreateDialog(projectId);
    gcy('ai-review-rule-form-overrides-toggle').click();
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
    gcy('ai-review-rule-form-overrides-toggle').click();
    gcy('ai-review-rule-form-override-add').click();
    gcy('ai-review-rule-form-override-row').should('have.length', 1);
    gcy('ai-review-rule-form-override-remove').click();
    gcy('ai-review-rule-form-override-row').should('not.exist');
  });

  it('branch fields are hidden when branching is off', () => {
    openCreateDialog(projectId);
    gcy('ai-review-rule-form-name').should('be.visible');
    gcy('ai-review-rule-form-branch-in').should('not.exist');
    gcy('ai-review-rule-form-branch-not-in').should('not.exist');
  });

  it('QA checkbox is hidden when QA is off', () => {
    openCreateDialog(projectId);
    gcy('ai-review-rule-form-name').should('be.visible');
    gcy('ai-review-rule-form-skip-qa').should('not.exist');
  });

  it('edit keeps values', () => {
    visitAiReviewRules(projectId);
    gcyAdvanced({ value: 'ai-review-rule-edit', name: 'UI rule' }).click();
    gcy('ai-review-rule-dialog').should('be.visible');
    gcy('ai-review-rule-form-name')
      .find('input')
      .should('have.value', 'UI rule');
    gcy('ai-review-rule-form-threshold')
      .find('input')
      .should('have.value', '85');
    gcy('ai-review-rule-form-save').click();
    waitForGlobalLoading();
    gcyAdvanced({ value: 'ai-review-rule-item', name: 'UI rule' }).should(
      'exist'
    );
  });

  it('threshold 101 shows a validation error', () => {
    openCreateDialog(projectId);
    gcy('ai-review-rule-form-threshold').find('input').clear().type('101');
    gcy('ai-review-rule-form-save').click();
    gcy('ai-review-rule-form-threshold-error').should('be.visible');
    gcy('ai-review-rule-dialog').should('be.visible');
  });
});
