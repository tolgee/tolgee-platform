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

export function countFilters(value: FiltersInternal) {
  return (
    getCommentsFiltersLength(value) +
    getNamespaceFiltersLength(value) +
    getScreenshotFiltersLength(value) +
    getTagFiltersLength(value) +
    getTranslationFiltersLength(value) +
    getLabelFiltersLength(value) +
    getSuggestionsFiltersLength(value)
  );
}

export function getFilterName(value: FiltersInternal) {
  return (
    getCommentsFiltersName(value) ||
    getNamespaceFiltersName(value) ||
    getScreenshotFiltersName(value) ||
    getTagFiltersName(value) ||
    getTranslationFiltersName(value) ||
    getLabelFiltersName(value) ||
    getSuggestionsFiltersName(value)
  );
}
