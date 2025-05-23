import { components } from '../../../../webapp/src/service/apiSchema.generated';
import { login } from '../../common/apiCalls/common';
import { prompt } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import { openBasicAiPrompt, toggleBasicOption } from '../../common/prompt';
import { gcy } from '../../common/shared';
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
    testOption('TM_SUGGESTIONS', 'Czech translation 2');
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
