import { type FiltersInternal } from './tools';
import {
  getCommentsFiltersLength,
  getCommentsFiltersName,
} from './SubfilterComments';
import {
  getNamespaceFiltersLength,
  getNamespaceFiltersName,
} from './SubfilterNamespaces';
import {
  getScreenshotFiltersLength,
  getScreenshotFiltersName,
} from './SubfilterScreenshots';
import {
  getDescriptionFiltersLength,
  getDescriptionFiltersName,
} from './SubfilterDescription';
import { getTagFiltersLength, getTagFiltersName } from './SubfilterTags';
import {
  getTranslationFiltersLength,
  getTranslationFiltersName,
} from './SubfilterTranslations';
import {
  getLabelFiltersLength,
  getLabelFiltersName,
} from 'tg.views/projects/translations/TranslationFilters/SubfilterLabels';
import {
  getSuggestionsFiltersLength,
  getSuggestionsFiltersName,
} from './SubfilterSuggestions';
import {
  getDeletedByFiltersLength,
  getDeletedByFiltersName,
} from './SubfilterDeletedBy';
import {
  getQaChecksFiltersLength,
  getQaChecksFiltersName,
  getTaskFiltersLength,
  getTaskFiltersName,
} from 'tg.ee';
import { components } from 'tg.service/apiSchema.generated';

type LabelModel = components['schemas']['LabelModel'];

export function countFilters(value: FiltersInternal) {
  return (
    getCommentsFiltersLength(value) +
    getNamespaceFiltersLength(value) +
    getScreenshotFiltersLength(value) +
    getDescriptionFiltersLength(value) +
    getTagFiltersLength(value) +
    getTranslationFiltersLength(value) +
    getLabelFiltersLength(value) +
    getSuggestionsFiltersLength(value) +
    getDeletedByFiltersLength(value) +
    getTaskFiltersLength(value) +
    getQaChecksFiltersLength(value)
  );
}

export function sameFilters(a: FiltersInternal, b: FiltersInternal) {
  const keys = new Set([...Object.keys(a), ...Object.keys(b)]);
  return [...keys].every(
    (key) =>
      JSON.stringify(a[key as keyof FiltersInternal]) ===
      JSON.stringify(b[key as keyof FiltersInternal])
  );
}

export function getFilterName(value: FiltersInternal, labels?: LabelModel[]) {
  return (
    getCommentsFiltersName(value) ||
    getNamespaceFiltersName(value) ||
    getScreenshotFiltersName(value) ||
    getDescriptionFiltersName(value) ||
    getTagFiltersName(value) ||
    getTranslationFiltersName(value) ||
    getLabelFiltersName(value, labels) ||
    getSuggestionsFiltersName(value) ||
    getDeletedByFiltersName(value) ||
    getTaskFiltersName(value) ||
    getQaChecksFiltersName(value)
  );
}
