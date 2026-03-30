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
  });

  afterEach(() => {
    qaTestData.clean();
  });

  it('shows QA badge on language row in dashboard', () => {
    login('test_username');
    cy.visit(`${HOST}/projects/${projectId}`);
    waitForGlobalLoading();

    // French language row should show QA badge since it has issues
    gcy('qa-badge').should('exist');
  });

  it('badge shows issue count', () => {
    login('test_username');
    cy.visit(`${HOST}/projects/${projectId}`);
    waitForGlobalLoading();

    // The badge should contain a number representing issue count
    gcy('qa-badge').first().find('.unresolved').should('exist');
  });
});
