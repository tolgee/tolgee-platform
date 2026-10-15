import { components } from '../../../webapp/src/service/apiSchema.generated';
import { internalFetch, login, v2apiFetch } from './apiCalls/common';
import { aiReviewTestData } from './apiCalls/testData/testData';
import { TestDataStandardResponse } from './apiCalls/testData/generator';
import { HOST } from './constants';
import { selectLangsInLocalstorage } from './translations';

type AiReviewRuleModel = components['schemas']['AiReviewRuleModel'];
type PromptModel = components['schemas']['PromptModel'];

export const AI_REVIEW_OWNER = 'ai_review_owner';
export const AI_REVIEW_ADMIN = 'ai_review_admin';
export const AI_REVIEW_TRANSLATOR = 'ai_review_translator';
export const AI_REVIEW_PROJECT = 'AI review project';
export const AI_REVIEW_LANGUAGES = ['en', 'de', 'cs', 'fr'];

export type AiReviewTestContext = {
  projectId: number;
  organizationSlug: string;
};

export function setupAiReviewTestData(
  username = AI_REVIEW_OWNER
): Cypress.Chainable<AiReviewTestContext> {
  aiReviewTestData.clean();
  return aiReviewTestData.generateStandard().then((res) => {
    const context = getContext(res.body);
    login(username);
    return cy.wrap(context);
  });
}

function getContext(data: TestDataStandardResponse): AiReviewTestContext {
  return {
    projectId: data.projects.find((p) => p.name === AI_REVIEW_PROJECT)!.id,
    organizationSlug: data.organizations.find(
      (o) => o.name === AI_REVIEW_OWNER
    )!.slug,
  };
}

export function setAiReviewFlagEnabledForAll(enabled: boolean) {
  return internalFetch(
    `e2e-data/ai-review/flag-enabled-for-all?enabled=${enabled}`,
    { method: 'PUT' }
  );
}

export function visitAiReviewTranslations(projectId: number, query?: string) {
  selectLangsInLocalstorage(projectId, AI_REVIEW_LANGUAGES);
  return cy.visit(
    `${HOST}/projects/${projectId}/translations${query ? `?${query}` : ''}`
  );
}

export function getRules(projectId: number) {
  return v2apiFetch(`projects/${projectId}/ai-review/rules`).then(
    (r) => (r.body._embedded?.rules ?? []) as AiReviewRuleModel[]
  );
}

export function getRule(projectId: number, name: string) {
  return getRules(projectId).then((rules) =>
    rules.find((rule) => rule.name === name)
  );
}

export function getReviewPrompts(projectId: number) {
  return v2apiFetch(`projects/${projectId}/prompts?type=REVIEW&size=100`).then(
    (r) => (r.body._embedded?.prompts ?? []) as PromptModel[]
  );
}
