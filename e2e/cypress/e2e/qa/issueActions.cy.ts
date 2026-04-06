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
    gcy('qa-check-item')
      .first()
      .findDcy('qa-action-ignore')
      .should('have.attr', 'data-cy-state', 'OPEN')
      .click();
    waitForGlobalLoading();

    // Issue should show as ignored
    gcy('qa-check-item')
      .first()
      .findDcy('qa-action-ignore')
      .should('have.attr', 'data-cy-state', 'IGNORED');
  });

  it('unignores a previously ignored issue', () => {
    login('test_username');
    const view = new E2TranslationsView();
    view.visit(projectId);

    view.getTranslationCell('key_ignored_issue', 'fr').click();
    openQaPanel();

    // The ignored issue should show the ignore button in ignored state
    gcy('qa-check-item')
      .first()
      .findDcy('qa-action-ignore')
      .should('have.attr', 'data-cy-state', 'IGNORED')
      .click();
    waitForGlobalLoading();

    // After unignoring, it should show the ignore button in open state
    gcy('qa-check-item')
      .first()
      .findDcy('qa-action-ignore')
      .should('have.attr', 'data-cy-state', 'OPEN');
  });

  it('corrects an issue with replacement text', () => {
    login('test_username');
    const view = new E2TranslationsView();
    view.visit(projectId);

    view.getTranslationCell('key_correctable', 'fr').click();
    openQaPanel();

    // Click the correct button
    gcy('qa-check-item').first().findDcy('qa-action-correct').click();

    // The editor should now contain the corrected text
    gcy('global-editor').should('contain.text', 'Bienvenue!');
  });
});
