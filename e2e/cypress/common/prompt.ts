import { gcy, gcyAdvanced } from './shared';
import { getTranslationCell } from './translations';
import { components } from '../../../webapp/src/service/apiSchema.generated';
import { LINKS, PARAMS } from '../../../webapp/src/constants/links';
import { buildXpath } from './XpathBuilder';
import { HOST } from './constants';

type BasicPromptOption =
  components['schemas']['PromptRunDto']['basicPromptOptions'][number];

export function openBasicAiPrompt() {
  getTranslationCell('Key 1', 'cs').click();
  gcy('llm-machine-translation-customize').click();
  gcy('ai-prompt-rendered-expand-button').click();
}

export function openAdvancedAiPlayground() {
  openBasicAiPrompt();
  gcy('ai-prompt-tab-advanced').click();
}

export function selectProvider(item: string) {
  gcy('ai-prompt-provider-select').click();
  gcy('ai-prompt-provider-item').contains(item).click();
}

export function getBasicOption(option: BasicPromptOption) {
  return gcyAdvanced({ value: 'prompt-basic-option', option });
}

export function toggleBasicOption(option: BasicPromptOption) {
  getBasicOption(option).click();
}

export function getPromptEditor() {
  cy.gcy('handlebars-editor').should('exist');
  return buildXpath()
    .descendant()
    .withDataCy('handlebars-editor')
    .descendant()
    .withAttribute('contenteditable')
    .getElement();
}

export function visitAiSettings(projectId: number) {
  return cy.visit(
    `${HOST}${LINKS.PROJECT_AI_PROMPTS.build({
      [PARAMS.PROJECT_ID]: projectId,
    })}`
  );
}
