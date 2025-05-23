import { components } from '../../../../webapp/src/service/apiSchema.generated';
import { login } from '../../common/apiCalls/common';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { prompt } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import { gcy, gcyAdvanced } from '../../common/shared';
import {
  getTranslationCell,
  visitTranslations,
} from '../../common/translations';

type BasicPromptOption =
  components['schemas']['PromptRunDto']['basicPromptOptions'][number];

describe('basic prompt', () => {
  let testData: TestDataStandardResponse;
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
        testData = data;
      });
  });

  it('options remove info from default prompt', () => {
    openAdvancedAiPlayground();
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
    toggleOption(option);
    gcy('ai-prompt-preview-button').click();
    waitForGlobalLoading();
    gcy('ai-prompt-rendered').should('not.contain', result);
  }

  function openAdvancedAiPlayground() {
    visitTranslations(testData.projects[1].id);
    getTranslationCell('Key 1', 'cs').click();
    gcy('llm-machine-translation-customize').click();
    gcy('ai-prompt-rendered-expand-button').click();
  }

  function toggleOption(option: BasicPromptOption) {
    gcyAdvanced({ value: 'prompt-basic-option', option }).click();
  }
});
