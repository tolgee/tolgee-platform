import { HOST } from './constants';
import { gcy, gcyAdvanced } from './shared';
import { getTranslationCell } from './translations';

export function openPlayground(
  projectId: number,
  keyName: string,
  languageTag = 'de'
) {
  cy.visit(
    `${HOST}/projects/${projectId}/translations?languages=en&languages=${languageTag}&search=${keyName}`
  );
  getTranslationCell(keyName, languageTag).click();
  gcy('llm-machine-translation-customize').click();
  gcy('ai-prompt-tab-basic').should('be.visible');
}

export function loadPrompt(name: string) {
  gcy('ai-prompt-open-existing-prompt-select').click();
  gcyAdvanced({ value: 'ai-prompt-open-existing-prompt-item', name }).click();
}
