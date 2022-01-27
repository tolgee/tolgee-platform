import { assertTooltip } from './shared';

export const stateColors = {
  Reviewed: 'rgb(23, 173, 24)',
  Translated: 'rgb(255, 206, 0)',
  'Machine translated': 'rgb(57, 225, 250)',
};

export const getCell = (translationText: string) => {
  return cy
    .contains(translationText)
    .should('be.visible')
    .closestDcy('translations-table-cell');
};

const getStateIndicator = (translationText: string) => {
  return getCell(translationText).findDcy('translations-state-indicator');
};

export const assertHasState = (
  translationText: string,
  stateName: keyof typeof stateColors
) => {
  getStateIndicator(translationText).trigger('mouseover');
  assertTooltip(stateName);
  getStateIndicator(translationText).trigger('mouseout');
};
