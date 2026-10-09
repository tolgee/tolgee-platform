import {
  findByExportParams,
  formatGroups,
  normalizeSelectedMessageFormat,
} from 'tg.views/projects/export/components/formatGroups';
import { API_DEFAULT_EXPORT_STATES } from 'tg.views/projects/export/exportSettings';
import { components } from 'tg.service/apiSchema.generated';

export function getCdEditInitialValues(
  data?: ContentDeliveryConfigModel,
  allowedTags?: string[],
  allNamespaces?: string[]
) {
  const initialFormat = data
    ? findByExportParams(data)
    : formatGroups[0].formats[0];

  return {
    name: data?.name ?? '',
    states: data?.filterState ?? API_DEFAULT_EXPORT_STATES,
    languages: data?.languages ?? allowedTags,
    format: initialFormat.id,
    namespaces: data?.filterNamespace ?? allNamespaces ?? [],
    tagsIn: getInitialTagsIn(data),
    tagsNotIn: data?.filterTagNotIn ?? [],
    autoPublish: data?.autoPublish ?? true,
    nested: initialFormat.structured ? data?.structureDelimiter === '.' : false,
    contentStorageId: data?.storage?.id,
    supportArrays:
      data?.supportArrays !== undefined
        ? data.supportArrays
        : initialFormat.defaultSupportArrays || false,
    messageFormat: normalizeSelectedMessageFormat({
      format: initialFormat.id,
      messageFormat: data?.messageFormat,
    }),
    pruneBeforePublish: data?.pruneBeforePublish ?? true,
    zip: data?.zip ?? false,
    escapeHtml: data?.escapeHtml ?? false,
    slug: data?.slug,
    filterBranch: (data?.branchName ?? data?.filterBranch ?? null) as
      | string
      | null,
  };
}

export type CdValues = ReturnType<typeof getCdEditInitialValues>;

function getInitialTagsIn(data?: ContentDeliveryConfigModel) {
  const tags = data?.filterTagIn ?? [];
  if (!data?.filterTag || tags.includes(data.filterTag)) {
    return tags;
  }
  return [...tags, data.filterTag];
}

export type ContentDeliveryConfigModel =
  components['schemas']['ContentDeliveryConfigModel'];
