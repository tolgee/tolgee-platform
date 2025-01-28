import { HOST } from './constants';

export const visitTasks = (projectId: number) => {
  return cy.visit(`${HOST}/projects/${projectId}/tasks`);
};

export const visitMyTasks = () => {
  return cy.visit(`${HOST}/my-tasks`);
};

export function getTaskPreview(language: string) {
  return cy
    .gcy('task-preview-language')
    .contains(language)
    .closestDcy('task-preview');
}

export function checkTaskPreview({
  language,
  keys,
  alert,
  words,
  characters,
}: {
  language: string;
  keys: number;
  alert: boolean;
  words: number;
  characters: number;
}) {
  getTaskPreview(language)
    .findDcy('task-preview-keys')
    .should('have.text', keys)
    .findDcy('task-preview-alert')
    .should(alert ? 'exist' : 'not.exist');
  getTaskPreview(language)
    .findDcy('task-preview-words')
    .should('have.text', words);
  getTaskPreview(language)
    .findDcy('task-preview-characters')
    .should('have.text', characters);
}
