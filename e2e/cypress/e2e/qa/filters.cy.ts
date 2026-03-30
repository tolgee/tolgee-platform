import {
  getProjectByNameFromTestData,
  qaTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { assertFilter } from '../../common/filters';
import { waitForGlobalLoading } from '../../common/loading';
import { HOST } from '../../common/constants';

describe('QA filtering', () => {
  let projectId: number;

  before(() => {
    qaTestData.clean();
    qaTestData.generateStandard().then((res) => {
      projectId = getProjectByNameFromTestData(res.body, 'test_project')!.id;
    });
  });

  beforeEach(() => {
    login('test_username');
    cy.visit(`${HOST}/projects/${projectId}/translations`);
    waitForGlobalLoading();
  });

  after(() => {
    qaTestData.clean();
  });

  it('filters by "Any QA issue"', () => {
    assertFilter({
      submenu: 'QA checks',
      filterOption: ['Any QA issue'],
      toSeeAfter: [
        'key_placeholder_issue',
        'key_punctuation_issue',
        'key_spacing_issue',
        'key_case_issue',
        'key_multiple_issues',
        'key_correctable',
      ],
    });
  });

  it('filters by specific check type', () => {
    assertFilter({
      submenu: 'QA checks',
      filterOption: ['Inconsistent placeholders'],
      toSeeAfter: ['key_placeholder_issue', 'key_multiple_issues'],
    });
  });
});
