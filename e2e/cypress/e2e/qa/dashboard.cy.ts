import {
  getProjectByNameFromTestData,
  qaTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';
import { HOST } from '../../common/constants';

describe('QA on dashboard', () => {
  let projectId: number;

  beforeEach(() => {
    qaTestData.clean();
    qaTestData.generateStandard().then((res) => {
      projectId = getProjectByNameFromTestData(res.body, 'test_project')!.id;
    });
    login('test_username');
  });

  afterEach(() => {
    qaTestData.clean();
  });

  it('shows QA badge with issue count on language row in dashboard', () => {
    cy.visit(`${HOST}/projects/${projectId}`);
    waitForGlobalLoading();

    // French language row should show QA badge since it has issues
    gcy('qa-badge').should('exist');
    // The badge should contain a number representing issue count
    gcy('qa-badge-unresolved').first().should('exist');
  });
});
