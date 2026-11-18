import {
  aiReviewTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy, visitProjectSettingsAdvanced } from '../../common/shared';
import {
  restoreAiReviewFeature,
  withoutAiReviewFeature,
} from '../../common/aiReview';
import { E2SourceChangeSettings } from '../../compounds/aiReview/E2SourceChangeSettings';

describe('AI review source change settings', () => {
  let projectId: number;
  let protectedProjectId: number;
  let switchOffProjectId: number;
  const settings = new E2SourceChangeSettings();

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
      switchOffProjectId = getProjectByNameFromTestData(
        res.body,
        'ai_review_switch_project'
      )!.id;
    });
    login('test_username');
  });

  afterEach(() => {
    restoreAiReviewFeature();
    aiReviewTestData.clean();
  });

  it('shows the defaults in the advanced project settings', () => {
    visitProjectSettingsAdvanced(projectId);
    gcy('ai-review-source-change-settings').should('be.visible');
    settings.getSelect('aiApproved', 'RETRANSLATE').should('exist');
    settings.getSelect('humanEdited', 'MARK_OUTDATED').should('exist');
    settings.getSelect('humanReviewed', 'MARK_OUTDATED').should('exist');
  });

  it('a maintainer changes a value and it survives a reload', () => {
    visitProjectSettingsAdvanced(projectId);
    settings.choose('humanEdited', 'RETRANSLATE');
    cy.reload();
    settings.getSelect('humanEdited', 'RETRANSLATE').should('exist');
  });

  it('hides the whole block without the AI review feature', () => {
    withoutAiReviewFeature();
    visitProjectSettingsAdvanced(projectId);
    gcy('project-settings-use-namespaces-switch').should('be.visible');
    gcy('ai-review-source-change-settings').should('not.exist');
  });

  it('hides the AI approved row when the project switch is off', () => {
    visitProjectSettingsAdvanced(switchOffProjectId);
    gcy('ai-review-source-change-settings').should('be.visible');
    settings.getSelect('humanEdited').should('exist');
    settings.getSelect('aiApproved').should('not.exist');
  });

  it('locks the protected rows under protect reviewed with helper texts', () => {
    visitProjectSettingsAdvanced(protectedProjectId);
    settings
      .getSelect('aiApproved')
      .should('have.attr', 'aria-disabled', 'true');
    settings
      .getSelect('humanReviewed')
      .should('have.attr', 'aria-disabled', 'true');
    settings
      .getSelect('humanEdited')
      .should('not.have.attr', 'aria-disabled', 'true');
    settings.getLockedHint('aiApproved').should('be.visible');
    settings.getLockedHint('humanReviewed').should('be.visible');
    settings.getSelect('aiApproved', 'RETRANSLATE').should('exist');
    settings.getSelect('humanReviewed', 'MARK_OUTDATED').should('exist');
  });
});
