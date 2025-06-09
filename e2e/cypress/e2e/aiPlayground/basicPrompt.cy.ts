import { components } from '../../../../webapp/src/service/apiSchema.generated';
import { login } from '../../common/apiCalls/common';
import { prompt } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import { openBasicAiPrompt, toggleBasicOption } from '../../common/prompt';
import { gcy, gcyAdvanced } from '../../common/shared';
import { visitTranslations } from '../../common/translations';

type BasicPromptOption =
  components['schemas']['PromptRunDto']['basicPromptOptions'][number];

describe('basic prompt', () => {
  beforeEach(() => {
    prompt.clean();
    prompt
      .generateStandard()
      .then((r) => r.body)
      .then((data) => {
        login(
          data.users.find((u) =>
            [u.username, u.name].includes('projectEditor@organization.com')
          )?.username
        );
        visitTranslations(
          data.projects.find((p) => p.name === 'Prompt project').id
        );
      });
  });

  it('options remove info from default prompt', () => {
    openBasicAiPrompt();
    testOption('KEY_NAME', '"Key 1"');
    testOption('KEY_DESCRIPTION', 'Key 1 description');
    testOption('PROJECT_DESCRIPTION', 'Project used for testing llms');
    testOption('LANGUAGE_NOTES', 'Language used for testing llms');
    testOption(
      'TM_SUGGESTIONS',
      'These are some results from translation memory'
    );
    testOption(
      'KEY_CONTEXT',
      'Here is list of translations used in the same context'
    );
    testOption('GLOSSARY', 'These glossary terms should be strictly used');
    testOption('SCREENSHOT', '[[screenshot_small_');
  });

  it('project description editable', () => {
    openBasicAiPrompt();
    gcyAdvanced({
      value: 'prompt-basic-option-edit',
      id: 'PROJECT_DESCRIPTION',
    }).click();
    gcy('project-ai-prompt-dialog-description-input')
      .clear()
      .type('My special project description');
    gcy('project-ai-prompt-dialog-save').click();
    waitForGlobalLoading();
    gcy('ai-prompt-preview-button').click();
    gcy('ai-prompt-rendered').should(
      'contain',
      'My special project description'
    );
  });

  function testOption(option: BasicPromptOption, result: string) {
    gcy('ai-prompt-preview-button').click();
    waitForGlobalLoading();
    gcy('ai-prompt-rendered').should('contain', result);
    toggleBasicOption(option);
    gcy('ai-prompt-preview-button').click();
    waitForGlobalLoading();
    gcy('ai-prompt-rendered').should('not.contain', result);
  }
});
