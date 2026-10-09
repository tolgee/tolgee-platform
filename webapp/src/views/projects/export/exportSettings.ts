import { EXPORTABLE_STATES, StateType } from 'tg.constants/translationStates';
import { queryDecode } from 'tg.hooks/useUrlSearchState';
import {
  formatGroups,
  getFormatById,
  getStructureDelimiter,
  MessageFormat,
  normalizeSelectedMessageFormat,
} from 'tg.views/projects/export/components/formatGroups';

export type ExportFormValues = {
  states: StateType[];
  languages: string[];
  format: string;
  namespaces: string[];
  tagsIn: string[];
  tagsNotIn: string[];
  nested: boolean;
  supportArrays: boolean;
  messageFormat: MessageFormat | undefined;
  escapeHtml: boolean;
};

export type StoredExportSettings = {
  states?: StateType[];
  languages?: string[] | null;
  format?: string;
  namespaces?: string[] | null;
  tagsIn?: string[];
  tagsNotIn?: string[];
  supportArrays?: boolean;
  messageFormat?: MessageFormat;
  escapeHtml?: boolean;
};

export const ALL_EXPORTABLE_STATES = EXPORTABLE_STATES as StateType[];

export const API_DEFAULT_EXPORT_STATES: StateType[] = [
  'TRANSLATED',
  'REVIEWED',
];

const DEFAULT_FORMAT = formatGroups[0].formats[0];

export function getDefaultExportValues(
  allLanguageTags: string[],
  allNamespaces: string[]
): ExportFormValues {
  return {
    states: ALL_EXPORTABLE_STATES,
    languages: allLanguageTags,
    format: DEFAULT_FORMAT.id,
    namespaces: allNamespaces,
    tagsIn: [],
    tagsNotIn: [],
    nested: false,
    supportArrays: DEFAULT_FORMAT.defaultSupportArrays || false,
    messageFormat: normalizeSelectedMessageFormat({
      format: DEFAULT_FORMAT.id,
      messageFormat: undefined,
    }),
    escapeHtml: false,
  };
}

export function restoreExportValues(
  stored: StoredExportSettings,
  allLanguageTags: string[],
  allNamespaces: string[]
): ExportFormValues {
  const defaults = getDefaultExportValues(allLanguageTags, allNamespaces);
  const format = formatExists(stored.format) ? stored.format! : defaults.format;
  return {
    ...defaults,
    states: keepKnown(stored.states, ALL_EXPORTABLE_STATES),
    languages: keepKnown(stored.languages, allLanguageTags),
    format,
    namespaces: keepKnown(stored.namespaces, allNamespaces),
    tagsIn: stored.tagsIn ?? [],
    tagsNotIn: stored.tagsNotIn ?? [],
    supportArrays:
      stored.supportArrays ??
      getFormatById(format).defaultSupportArrays ??
      false,
    messageFormat: normalizeSelectedMessageFormat({
      format,
      messageFormat: stored.messageFormat,
    }),
    escapeHtml: stored.escapeHtml ?? false,
  };
}

export function toStoredExportSettings(
  values: ExportFormValues,
  allLanguageTags: string[],
  allNamespaces: string[]
): StoredExportSettings {
  return {
    states: values.states,
    languages: nullIfAll(values.languages, allLanguageTags),
    format: values.format,
    namespaces: nullIfAll(values.namespaces, allNamespaces),
    tagsIn: values.tagsIn,
    tagsNotIn: values.tagsNotIn,
    supportArrays: values.supportArrays,
    messageFormat: getEffectiveMessageFormat(values),
    escapeHtml: values.escapeHtml,
  };
}

export function getEffectiveMessageFormat(values: ExportFormValues) {
  if (!getFormatById(values.format).supportedMessageFormats) {
    return undefined;
  }
  return normalizeSelectedMessageFormat(values);
}

export type TagFilterValues = {
  tagsIn: string[];
  tagsNotIn: string[];
};

export function getTagFilterParams(values: TagFilterValues) {
  return {
    filterTagIn: values.tagsIn.length ? values.tagsIn : undefined,
    filterTagNotIn: values.tagsNotIn.length ? values.tagsNotIn : undefined,
  };
}

export function getExportRequestBody(
  values: ExportFormValues,
  branchName: string | undefined
) {
  const format = getFormatById(values.format);
  return {
    format: format.format,
    filterState: values.states,
    languages: values.languages,
    structureDelimiter: getStructureDelimiter(format),
    filterNamespace: values.namespaces,
    ...getTagFilterParams(values),
    filterBranch: branchName,
    zip: values.languages.length > 1 || values.namespaces.length > 1,
    supportArrays: values.supportArrays || false,
    messageFormat:
      format.messageFormat ?? normalizeSelectedMessageFormat(values),
    escapeHtml: values.escapeHtml || false,
  };
}

export function readExportSettingsFromUrl(
  search: string
): StoredExportSettings {
  const query = queryDecode(search, true) as Record<
    string,
    string[] | undefined
  >;
  const settings: StoredExportSettings = {
    states: query.states as StateType[] | undefined,
    languages: query.languages,
    format: query.format?.[0],
    messageFormat: query.messageFormat?.[0] as MessageFormat | undefined,
    tagsIn: query.tagsIn,
    tagsNotIn: query.tagsNotIn,
  };
  return Object.fromEntries(
    Object.entries(settings).filter(([, value]) => value !== undefined)
  );
}

export function isSameExportSetup(a: ExportFormValues, b: ExportFormValues) {
  return (
    sameItems(a.states, b.states) &&
    sameItems(a.languages, b.languages) &&
    a.format === b.format &&
    sameItems(a.namespaces, b.namespaces) &&
    sameItems(a.tagsIn, b.tagsIn) &&
    sameItems(a.tagsNotIn, b.tagsNotIn) &&
    a.supportArrays === b.supportArrays &&
    getEffectiveMessageFormat(a) === getEffectiveMessageFormat(b) &&
    a.escapeHtml === b.escapeHtml
  );
}

function formatExists(formatId: string | undefined) {
  if (!formatId) {
    return false;
  }
  return formatGroups.some((group) =>
    group.formats.some((format) => format.id === formatId)
  );
}

function keepKnown<T extends string>(
  stored: T[] | null | undefined,
  known: T[]
): T[] {
  const kept = (stored ?? []).filter((item) => known.includes(item));
  if (!kept.length) {
    return known;
  }
  return kept;
}

function nullIfAll(selected: string[], all: string[]) {
  if (all.every((item) => selected.includes(item))) {
    return null;
  }
  return selected;
}

function sameItems(a: string[], b: string[]) {
  return a.length === b.length && a.every((item) => b.includes(item));
}
