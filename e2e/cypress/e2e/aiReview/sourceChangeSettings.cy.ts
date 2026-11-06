import {
  aiReviewTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { HOST } from '../../common/constants';
import { gcy, gcyAdvanced } from '../../common/shared';
import {
  restoreAiReviewFeature,
  withoutAiReviewFeature,
} from '../../common/aiReview';
import { waitForGlobalLoading } from '../../common/loading';

function visitAdvancedSettings(projectId: number) {
  return cy.visit(`${HOST}/projects/${projectId}/manage/edit/advanced`);
}

describe('AI review source change settings', () => {
  let projectId: number;
  let protectedProjectId: number;

  beforeEach(() => {
    aiReviewTestData.clean();
    aiReviewTestData.generateStandard().then((res) => {
      projectId = getProjectByNameFromTestData(
        res.body,
        'ai_review_project'
      )!.id;
      protectedProjectId = getProjectByNameFromTestData(
        res.body,
        'ai_review_empty_project'
      )!.id;
    });
    login('test_username');
  });

  afterEach(() => {
    restoreAiReviewFeature();
    aiReviewTestData.clean();
  });

  it('shows the defaults in the advanced project settings', () => {
    visitAdvancedSettings(projectId);
    gcy('ai-review-source-change-settings').should('be.visible');
    gcyAdvanced({
      value: 'source-change-select',
      field: 'aiApproved',
      selected: 'RETRANSLATE',
    }).should('exist');
    gcyAdvanced({
      value: 'source-change-select',
      field: 'humanEdited',
      selected: 'MARK_OUTDATED',
    }).should('exist');
    gcyAdvanced({
      value: 'source-change-select',
      field: 'humanReviewed',
      selected: 'MARK_OUTDATED',
    }).should('exist');
  });

  it('a maintainer changes a value and it survives a reload', () => {
    visitAdvancedSettings(projectId);
    gcyAdvanced({
      value: 'source-change-select',
      field: 'humanEdited',
    }).click();
    gcyAdvanced({
      value: 'source-change-select-item',
      action: 'RETRANSLATE',
    }).click();
    waitForGlobalLoading();
    cy.reload();
    gcyAdvanced({
      value: 'source-change-select',
      field: 'humanEdited',
      selected: 'RETRANSLATE',
    }).should('exist');
  });

  it('hides the AI approved row without the AI review feature', () => {
    withoutAiReviewFeature();
    visitAdvancedSettings(projectId);
    gcy('ai-review-source-change-settings').should('be.visible');
    gcyAdvanced({
      value: 'source-change-select',
      field: 'humanEdited',
    }).should('exist');
    gcyAdvanced({
      value: 'source-change-select',
      field: 'aiApproved',
    }).should('not.exist');
  });

  it('locks the protected rows under protect reviewed with helper texts', () => {
    visitAdvancedSettings(protectedProjectId);
    gcyAdvanced({ value: 'source-change-select', field: 'aiApproved' }).should(
      'have.attr',
      'aria-disabled',
      'true'
    );
    gcyAdvanced({
      value: 'source-change-select',
      field: 'humanReviewed',
    }).should('have.attr', 'aria-disabled', 'true');
    gcyAdvanced({ value: 'source-change-select', field: 'humanEdited' }).should(
      'not.have.attr',
      'aria-disabled',
      'true'
    );
    gcyAdvanced({
      value: 'source-change-locked-hint',
      field: 'aiApproved',
    }).should('be.visible');
    gcyAdvanced({
      value: 'source-change-locked-hint',
      field: 'humanReviewed',
    }).should('be.visible');
    gcyAdvanced({
      value: 'source-change-select',
      field: 'aiApproved',
      selected: 'RETRANSLATE',
    }).should('exist');
    gcyAdvanced({
      value: 'source-change-select',
      field: 'humanReviewed',
      selected: 'MARK_OUTDATED',
    }).should('exist');
  });
});
