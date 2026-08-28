import { gcy } from './shared';

export const submitFormDirectly = () =>
  gcy('standard-form')
    .should('have.length', 1)
    .then(($form) => ($form[0] as HTMLFormElement).requestSubmit());
