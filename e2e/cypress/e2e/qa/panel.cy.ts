import {
  getProjectByNameFromTestData,
  qaTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy, gcyAdvanced } from '../../common/shared';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';

describe('QA panel', () => {
  let projectId: number;
  let disabledProjectId: number;
  let view: E2TranslationsView;

  beforeEach(() => {
    qaTestData.clean();
    qaTestData.generateStandard().then((res) => {
      projectId = getProjectByNameFromTestData(res.body, 'test_project')!.id;
      disabledProjectId = getProjectByNameFromTestData(
        res.body,
        'Disabled QA Project'
      )!.id;
    });
    login('test_username');
    view = new E2TranslationsView();
  });

  afterEach(() => {
    qaTestData.clean();
  });

  it('shows QA issues in panel when editing translation with issues', () => {
    view.visit(projectId);

    view.getTranslationCell('key_placeholder_issue', 'fr').click();

    gcyAdvanced({
      value: 'translation-panel-toggle',
      id: 'qa_checks',
    }).click();

    gcy('qa-panel-container').should('exist');
    gcy('qa-check-item').should('have.length', 1);
  });

  it('shows empty panel when editing translation without issues', () => {
    view.visit(projectId);

    view.getTranslationCell('key_no_issues', 'fr').click();

    gcyAdvanced({
      value: 'translation-panel-toggle',
      id: 'qa_checks',
    }).click();

    gcy('qa-panel-container-empty').should('exist');
    gcy('qa-check-item').should('not.exist');
  });

  it('shows project-disabled message when QA is off for the project', () => {
    view.visit(disabledProjectId);

    view.getTranslationCell('disabled_key', 'fr').click();

    gcyAdvanced({
      value: 'translation-panel-toggle',
      id: 'qa_checks',
    }).click();

    gcy('qa-panel-container-project-disabled').should('exist');
  });

  it('shows multiple issues for translation with multiple problems', () => {
    view.visit(projectId);

    view.getTranslationCell('key_multiple_issues', 'fr').click();

    gcyAdvanced({
      value: 'translation-panel-toggle',
      id: 'qa_checks',
    }).click();

    gcy('qa-panel-container').should('exist');
    gcy('qa-check-item').should('have.length', 4);
  });
});
