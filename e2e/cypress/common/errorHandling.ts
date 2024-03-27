export type Config = {
  endpoint: string;
  method?: string;
  statusCode: number;
  body?: any;
  times?: number;
};

export function simulateError({
  method,
  endpoint,
  statusCode,
  body,
  ...rest
}: Config) {
  cy.intercept(
    {
      method: method,
      url: `*${endpoint}*`,
      ...rest,
    },
    { statusCode, body }
  );
}

export function tryCreateProject(name: string) {
  cy.gcy('project-name-field').type('Test');
  cy.gcy('global-form-save-button').click();
}
