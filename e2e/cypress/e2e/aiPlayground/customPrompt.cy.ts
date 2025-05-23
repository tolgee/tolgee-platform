import { login } from '../../common/apiCalls/common';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { prompt } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import { gcy } from '../../common/shared';
import {
  getTranslationCell,
  visitTranslations,
} from '../../common/translations';
import { buildXpath } from '../../common/XpathBuilder';

describe('custom prompt', () => {
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

  it('sends simple prompt', () => {
    openAdvancedAiPlayground();
    getPromptEditor().clear().type("Hi please don't kill us");
    gcy('ai-prompt-preview-button').click();
    waitForGlobalLoading();

    // check rendered prompt
    gcy('ai-prompt-rendered-expand-button').click();
    gcy('ai-prompt-rendered').should('contain', "Hi please don't kill us");

    // check usage
    gcy('ai-playground-usage-tokens').should('contain', '63');
    gcy('ai-playground-usage-cached-tokens').should('contain', '1');
    gcy('ai-playground-usage-mt-credits').should('contain', '113.5');

    // check translation result
    gcy('ai-prompt-result-translation').should(
      'contain',
      'response from: server-provider'
    );
    gcy('ai-prompt-result-translation').should(
      'contain',
      'description from: server-provider'
    );

    // check raw result
    gcy('ai-prompt-show-result-toggle').click();
    gcy('ai-prompt-result-raw').should(
      'contain',
      '"output": "response from: server-provider"'
    );
    gcy('ai-prompt-result-raw').should(
      'contain',
      '"contextDescription": "context description from: server-provider"'
    );
  });

  it('default prompt fragments are present', () => {
    openAdvancedAiPlayground();
    gcy('ai-prompt-preview-button').click();
    waitForGlobalLoading();

    gcy('ai-prompt-rendered-expand-button').click();
    // fragments.intro
    gcy('ai-prompt-rendered').should('contain', 'You are a translator');

    // fragments.styledInfo
    gcy('ai-prompt-rendered').should('contain', "Don't add any extra dots");

    // fragments.projectDescription
    gcy('ai-prompt-rendered').should(
      'contain',
      'Project used for testing llms'
    );

    // fragments.languageNotes
    gcy('ai-prompt-rendered').should(
      'contain',
      'Language used for testing llms'
    );

    // fragments.translationMemory
    gcy('ai-prompt-rendered').should('contain', 'Czech translation 2');
    gcy('ai-prompt-rendered').should('contain', 'English translation 2');

    // fragments.relatedKeys
    // gcy('ai-prompt-rendered').should('contain', 'You are a translator');

    // fragments.icuInfo
    gcy('ai-prompt-rendered').should(
      'contain',
      'If message includes ICU parameters'
    );

    // fragments.keyName
    gcy('ai-prompt-rendered').should('contain', '"Key 1"');

    // fragments.keyDescription
    gcy('ai-prompt-rendered').should('contain', 'Key 1 description.');

    // fragments.screenshot
    // gcy('ai-prompt-rendered').should('contain', 'Key 1 description.');

    // fragments.translateJson
    gcy('ai-prompt-rendered').should('contain', 'Follow this json format');
  });

  it('prompt key variables work', () => {
    openAdvancedAiPlayground();
    testVariables({
      '{{key.name}}': 'Key 1',
      '{{key.description}}': 'Key 1 description.',
    });
  });

  it('prompt other variables work', () => {
    openAdvancedAiPlayground();
    testVariables({
      '{{other.de.languageTag}}': 'de',
      '{{other.de.languageName}}': 'German',
      '{{other.de.translation}}': 'German translation 1',
      '{{#if other.de.isCJK}} Is CJK {{else}} Not CJK {{/if}}': 'Not CJK',
      '{{other.zh.languageTag}}': 'zh',
      '{{other.zh.languageName}}': 'Chinese',
      '{{other.zh.translation}}': 'Chinese translation 1',
      '{{#if other.zh.isCJK}} Is CJK {{else}} Not CJK {{/if}}': 'Is CJK',
    });
  });

  it('prompt project variables work', () => {
    openAdvancedAiPlayground();
    testVariables({
      '{{project.name}}': 'Prompt project',
      '{{project.description}}': 'Project used for testing llms',
    });
  });

  it('prompt source variables work', () => {
    openAdvancedAiPlayground();
    testVariables({
      '{{source.languageTag}}': 'en',
      '{{source.languageName}}': 'English',
      '{{source.translation}}': 'English translation 1',
      '{{#if source.isCJK}} Is CJK {{else}} Not CJK {{/if}}': 'Not CJK',
    });
  });

  it('prompt target variables work', () => {
    openAdvancedAiPlayground();
    testVariables({
      '{{target.languageTag}}': 'cs',
      '{{target.languageName}}': 'Czech',
      '{{target.translation}}': 'Czech translation 1',
      '{{#if target.isCJK}} Is CJK {{else}} Not CJK {{/if}}': 'Not CJK',
    });
  });

  it('prompt other variables work', () => {
    openAdvancedAiPlayground();
    testVariables({
      '{{translationMemory.json}}': 'Czech translation 2',
    });
  });

  function testVariables(data: Record<string, string>) {
    const variables = Object.keys(data)
      .map((varName) => varName)
      .join('\n');

    getPromptEditor()
      .clear()
      .type(variables, { parseSpecialCharSequences: false });
    gcy('ai-prompt-preview-button').click();
    waitForGlobalLoading();
    gcy('ai-prompt-rendered-expand-button').click();

    const results = Object.values(data);
    results.forEach((result) => {
      gcy('ai-prompt-rendered').should('contain', result);
    });
  }

  function openAdvancedAiPlayground() {
    visitTranslations(testData.projects[1].id);
    getTranslationCell('Key 1', 'cs').click();
    gcy('llm-machine-translation-customize').click();
    gcy('ai-prompt-tab-advanced').click();
  }

  function getPromptEditor() {
    cy.gcy('handlebars-editor').should('exist');
    return buildXpath()
      .descendant()
      .withDataCy('handlebars-editor')
      .descendant()
      .withAttribute('contenteditable')
      .getElement();
  }
});
