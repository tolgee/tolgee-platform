import { gcy, selectInSelect } from './shared';

export function formatCurrentDatePlusDays(days) {
  return new Intl.DateTimeFormat('en', {
    dateStyle: 'full',
  }).format(new Date().getTime() + days * 24 * 60 * 60 * 1000 - 2000);
}

export function setExpiration(selectValue: string, customDate?: string) {
  selectInSelect(gcy('expiration-select'), selectValue);
  if (selectValue == 'Custom date') {
    cy.chooseDatePicker('[data-cy="expiration-date-picker"]', customDate);
  }
}
