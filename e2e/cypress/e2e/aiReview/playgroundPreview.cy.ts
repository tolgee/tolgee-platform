import {
  aiReviewTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy, gcyAdvanced } from '../../common/shared';
import { loadPrompt, openPlayground } from '../../common/aiPlayground';

describe('AI review playground preview', () => {
  let projectId: number;

  beforeEach(() => {
    aiReviewTestData.clean();
    aiReviewTestData.generateStandard().then((res) => {
      projectId = getProjectByNameFromTestData(
        res.body,
        'ai_review_project'
      )!.id;
    });
    login('test_username');
  });

  afterEach(() => {
    aiReviewTestData.clean();
  });

  it('a seeded review result shows the score and the issue list', () => {
    openPlayground(projectId, 'key_untagged');
    gcyAdvanced({
      value: 'ai-playground-preview-score',
      score: '55',
    }).should('be.visible');
    gcy('ai-review-issue').should('have.length', 2);
    gcyAdvanced({
      value: 'ai-review-issue',
      severity: 'critical',
      points: '35',
    }).should('be.visible');
  });

  it('batch preview shows the score and no issues found', () => {
    openPlayground(projectId, 'key_untagged');
    loadPrompt('Brand review');
    gcy('ai-prompt-preview-more-button').click();
    gcy('ai-prompt-preview-on-all').click();
    gcy('ai-prompt-batch-dialog-run').click();
    gcyAdvanced(
      { value: 'ai-playground-preview-score', score: '100' },
      { timeout: 60_000 }
    ).should('be.visible');
    gcy('ai-playground-preview-no-issues').should('be.visible');
  });

  it('does not show review badges in the playground', () => {
    openPlayground(projectId, 'key_approved');
    gcy('ai-playground-preview').should('not.exist');
    gcy('ai-review-badge').should('not.exist');
  });
});
