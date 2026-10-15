import { HOST } from '../common/constants';
import { E2KeyCreateDialog, KeyDialogFillProps } from './E2KeyCreateDialog';
import { getTranslationCell } from '../common/translations';
import { gcy, gcyAdvanced } from '../common/shared';
import { E2GlossaryPanel } from './glossaries/E2GlossaryPanel';
import {
  components,
  operations,
} from '../../../webapp/src/service/apiSchema.generated';
import { waitForGlobalLoading } from '../common/loading';

type AiReviewOutcome =
  components['schemas']['TranslationAiReviewModel']['outcome'];
type TranslationState = components['schemas']['TranslationViewModel']['state'];
export type AiReviewFilterOutcome = NonNullable<
  operations['getTranslations']['parameters']['query']['filterAiReviewOutcome']
>[number];

export class E2TranslationsView {
  visit(projectId: number) {
    return cy.visit(`${HOST}/projects/${projectId}/translations`);
  }

  getAddButton() {
    return cy.gcy('translations-add-button');
  }

  openKeyCreateDialog() {
    this.getAddButton().click();
    return new E2KeyCreateDialog();
  }

  createKey(props: KeyDialogFillProps) {
    const dialog = this.openKeyCreateDialog();
    dialog.fillAndSave(props);
  }

  getTranslationCell(key: string, languageTag: string) {
    return getTranslationCell(key, languageTag);
  }

  closeTranslationEdit() {
    gcy('translations-cell-cancel-button').click();
  }

  openFilterSelect() {
    gcy('translations-filter-select').click();
    cy.waitForDom();
    return this;
  }

  selectLabelsFilter() {
    this.getLabelsFilter().should('exist').click();
    return this;
  }

  getLabelsFilter() {
    return gcy('submenu-item').contains('Labels');
  }

  selectLabelByName(labelName: string) {
    gcy('filter-item').contains(labelName).click();
    return this;
  }

  applyFilterForExpand() {
    gcy('translations-filter-apply-for-expand').click();
    return this;
  }

  applyFilterForAll() {
    gcy('translations-filter-apply-for-all').click();
    return this;
  }

  applyFilterForLanguage(language: string) {
    gcy('translations-filter-apply-for-language').contains(language).click();
    return this;
  }

  closeFilterMenus() {
    cy.get('body').type('{esc}');
    cy.get('body').type('{esc}');
    waitForGlobalLoading();
    return this;
  }

  openAiReviewFilter() {
    this.openFilterSelect();
    gcy('translations-filter-ai-review').click();
    gcy('translations-filter-ai-review-menu').should('be.visible');
    return this;
  }

  selectAiReviewOutcome(outcome: AiReviewFilterOutcome) {
    gcyAdvanced({
      value: 'translations-filter-ai-review-outcome',
      outcome,
    }).click();
    return this;
  }

  filterByAiReviewOutcomes(...outcomes: AiReviewFilterOutcome[]) {
    this.openAiReviewFilter();
    outcomes.forEach((outcome) => this.selectAiReviewOutcome(outcome));
    return this.closeFilterMenus();
  }

  selectAiReviewRule(ruleName: string) {
    gcy('translations-filter-ai-review-by-rule').click();
    gcyAdvanced({
      value: 'translations-filter-ai-review-rule',
      rule: ruleName,
    }).click();
    return this;
  }

  selectAiReviewScoreBelow(score: number) {
    gcy('translations-filter-ai-review-score-below').click();
    gcyAdvanced({
      value: 'translations-filter-ai-review-score-option',
      score: String(score),
    }).click();
    return this;
  }

  applyAiReviewFilterForLanguage(languageTag: string) {
    gcyAdvanced({
      value: 'translations-filter-apply-for-language',
      language: languageTag,
    }).click();
    return this;
  }

  selectRowCheckbox(key: string) {
    this.getKeyCell(key).findDcy('translations-row-checkbox').click();
    return this;
  }

  assertVisibleKeys(...keys: string[]) {
    this.assertTranslationsRowsCount(keys.length);
    keys.forEach((key) => this.getKeyCell(key).should('exist'));
  }

  getTranslationsRows() {
    return gcy('translations-row');
  }

  assertTranslationsRowsCount(count: number) {
    this.getTranslationsRows().should('have.length', count);
  }

  filterByLabel(labelName: string) {
    this.openFilterSelect().selectLabelsFilter().selectLabelByName(labelName);
    gcy('translations-filter-select').contains(labelName);
    return this;
  }

  openGlossaryPanel() {
    gcyAdvanced({
      value: 'translation-panel-toggle',
      id: 'glossaries',
    }).click();
    gcy('glossary-panel-container').should('exist');
    return new E2GlossaryPanel();
  }

  getSearchField() {
    return gcy('global-search-field').find('.cm-content');
  }

  getKeyCell(key: string) {
    return gcyAdvanced({ value: 'translations-table-cell', key });
  }

  getKeyLanguageCell(key: string, languageTag: string) {
    return gcyAdvanced({
      value: 'translations-table-cell',
      key,
      language: languageTag,
    });
  }

  getAiReviewBadge(key: string, languageTag: string) {
    return this.getKeyLanguageCell(key, languageTag).findDcy(
      'translations-cell-ai-review-badge'
    );
  }

  assertAiReviewBadge(
    key: string,
    languageTag: string,
    outcome: AiReviewOutcome,
    score?: number
  ) {
    this.getAiReviewBadge(key, languageTag)
      .should('be.visible')
      .should('have.attr', 'data-cy-outcome', outcome)
      .should('have.attr', 'data-cy-score', score === undefined ? '' : score);
  }

  openAiReviewPopover(key: string, languageTag: string) {
    this.getAiReviewBadge(key, languageTag).click();
    return gcy('ai-review-popover').should('be.visible');
  }

  assertTranslationState(
    key: string,
    languageTag: string,
    state: TranslationState
  ) {
    this.getKeyLanguageCell(key, languageTag)
      .closestDcy('translations-table-cell-translation')
      .findDcy('translations-state-indicator')
      .should('have.attr', 'data-cy-state', state);
  }

  clickStateButton(key: string, languageTag: string) {
    this.getKeyLanguageCell(key, languageTag)
      .closestDcy('translations-table-cell-translation')
      .trigger('mouseover')
      .findDcy('translation-state-button')
      .click();
  }

  searchFor(query: string) {
    this.getSearchField().type(query);
    return this;
  }

  clearSearchWithButton() {
    gcy('global-search-field-clear').click();
    return this;
  }

  getSearchSuggestions() {
    return cy.get('.cm-tooltip-autocomplete');
  }

  getSearchHelp() {
    gcy('translations-search-help-button').click();
    return gcy('translations-search-help-popover');
  }
}
