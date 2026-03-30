import {
  getProjectByNameFromTestData,
  qaTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy, gcyAdvanced } from '../../common/shared';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';
import { waitForGlobalLoading } from '../../common/loading';

describe('QA issue actions', () => {
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

  function openQaPanel() {
    gcyAdvanced({
      value: 'translation-panel-toggle',
      id: 'qa_checks',
    }).click();
    gcy('qa-panel-container').should('exist');
  }

  it('ignores a QA issue', () => {
    login('test_username');
    const view = new E2TranslationsView();
    view.visit(projectId);

    view.getTranslationCell('key_correctable', 'fr').click();
    openQaPanel();

    // Click the ignore button
    gcy('qa-check-item').first().contains('Ignore').click();
    waitForGlobalLoading();

    // Issue should show as ignored (with reduced opacity container)
    gcy('qa-check-item').first().contains('Unignore').should('exist');
  });

  it('unignores a previously ignored issue', () => {
    login('test_username');
    const view = new E2TranslationsView();
    view.visit(projectId);

    view.getTranslationCell('key_ignored_issue', 'fr').click();
    openQaPanel();

    // The ignored issue should show unignore button
    gcy('qa-check-item').first().contains('Unignore').click();
    waitForGlobalLoading();

    // After unignoring, it should show the ignore button again
    gcy('qa-check-item').first().contains('Ignore').should('exist');
  });

  it('corrects an issue with replacement text', () => {
    login('test_username');
    const view = new E2TranslationsView();
    view.visit(projectId);

    view.getTranslationCell('key_correctable', 'fr').click();
    openQaPanel();

    // Click the correct button
    gcy('qa-check-item').first().contains('Correct').click();

    // The editor should now contain the corrected text
    gcy('global-editor').should('contain.text', 'Bienvenue!');
  });
});
