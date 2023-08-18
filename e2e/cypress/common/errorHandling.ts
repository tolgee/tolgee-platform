export type Config = {
  endpoint: string;
  method?: string;
  statusCode: number;
  body?: any;
};

export function simulateError({ method, endpoint, statusCode, body }: Config) {
  cy.intercept(
    {
      method: method,
      url: `*${endpoint}*`,
    },
    { statusCode, body }
  );
}

export function tryCreateProject(name: string) {
  cy.gcy('project-name-field').type('Test');
  cy.gcy('global-form-save-button').click();
}
