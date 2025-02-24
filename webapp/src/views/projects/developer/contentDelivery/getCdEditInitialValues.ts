import {
  findByExportParams,
  formatGroups,
  normalizeSelectedMessageFormat,
} from '../../export/components/formatGroups';
import { EXPORTABLE_STATES, StateType } from 'tg.constants/translationStates';
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
    states: data?.filterState ?? EXPORT_DEFAULT_STATES,
    languages: data?.languages ?? allowedTags,
    format: initialFormat.id,
    namespaces: data?.filterNamespace ?? allNamespaces ?? [],
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
    escapeHtml: data?.escapeHtml ?? false,
    slug: data?.slug,
  };
}

export type CdValues = ReturnType<typeof getCdEditInitialValues>;

function sortStates(arr: StateType[]) {
  return [...arr].sort(
    (a, b) => EXPORTABLE_STATES.indexOf(a) - EXPORTABLE_STATES.indexOf(b)
  );
}

const EXPORT_DEFAULT_STATES: StateType[] = sortStates([
  'TRANSLATED',
  'REVIEWED',
]);

export type ContentDeliveryConfigModel =
  components['schemas']['ContentDeliveryConfigModel'];
