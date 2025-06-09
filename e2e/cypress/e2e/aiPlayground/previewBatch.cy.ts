import { login } from '../../common/apiCalls/common';
import { prompt } from '../../common/apiCalls/testData/testData';
import { openBasicAiPrompt } from '../../common/prompt';
import { gcy } from '../../common/shared';
import { createTag } from '../../common/tags';
import {
  selectLangsInLocalstorage,
  visitTranslations,
} from '../../common/translations';

describe('preview batch', () => {
  beforeEach(() => {
    prompt.clean();
    prompt
      .generateStandard()
      .then((r) => r.body)
      .then((data) => {
        const project = data.projects.find((p) => p.name === 'Prompt project');
        login(
          data.users.find((u) =>
            [u.username, u.name].includes('projectEditor@organization.com')
          )?.username
        );
        selectLangsInLocalstorage(project.id, ['en', 'cs', 'de']);
        visitTranslations(project.id);
      });
  });

  it('previews batch on all items', () => {
    openBasicAiPrompt();
    gcy('ai-prompt-preview-more-button').click();
    gcy('ai-prompt-preview-on-all').click();
    gcy('ai-prompt-batch-dialog-run').click();
    gcy('ai-playground-preview')
      .should('contain', 'response from: server-provider')
      .should('have.length', 8);
  });

  it('previews batch on tagged', () => {
    createTag('ai-playground');
    openBasicAiPrompt();
    gcy('ai-prompt-preview-more-button').click();
    gcy('ai-prompt-preview-on-dataset').click();
    gcy('ai-prompt-dataset-run').click();
    gcy('ai-playground-preview')
      .should('contain', 'response from: server-provider')
      .should('have.length', 2);
  });

  it('shows hint for user when nothing tagged', () => {
    openBasicAiPrompt();
    gcy('ai-prompt-preview-more-button').click();
    gcy('ai-prompt-preview-on-dataset').click();
    gcy('ai-prompt-dataset-no-tags-text')
      .should('be.visible')
      .should('contain', 'ai-playground');
  });
});
