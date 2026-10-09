import { ReactNode } from 'react';
import { useTranslate } from '@tolgee/react';

import { useProject } from 'tg.hooks/useProject';
import { useStateTranslation } from 'tg.translationTools/useStateTranslation';
import {
  getFormatById,
  normalizeSelectedMessageFormat,
} from 'tg.views/projects/export/components/formatGroups';
import { messageFormatTranslation } from 'tg.views/projects/export/components/messageFormatTranslation';
import {
  ALL_EXPORTABLE_STATES,
  ExportFormValues,
} from 'tg.views/projects/export/exportSettings';

export type SummaryItem = {
  text: ReactNode;
  state: 'default' | 'changed' | 'invalid';
};

export function useAdvancedSettingsSummary(
  values: ExportFormValues,
  allNamespaces: string[] | undefined
): SummaryItem[] {
  const { t } = useTranslate();
  const project = useProject();
  const translateState = useStateTranslation();

  const items: SummaryItem[] = [getStatesItem()];
  if (project.useNamespaces && allNamespaces?.length) {
    items.push(getNamespacesItem(allNamespaces));
  }
  items.push(...getFormatOptionItems());
  items.push(...getTagItems());
  return items;

  function getStatesItem(): SummaryItem {
    if (!values.states.length) {
      return {
        text: t('export_advanced_summary_no_state', 'No state selected'),
        state: 'invalid',
      };
    }
    if (ALL_EXPORTABLE_STATES.every((s) => values.states.includes(s))) {
      return {
        text: t('export_advanced_summary_all_states', 'All states'),
        state: 'default',
      };
    }
    return {
      text: values.states.map((state) => translateState(state)).join(', '),
      state: 'changed',
    };
  }

  function getNamespacesItem(namespaces: string[]): SummaryItem {
    const selected = values.namespaces.length;
    if (!selected || selected === namespaces.length) {
      return {
        text: t('export_advanced_summary_all_namespaces', 'All namespaces'),
        state: 'default',
      };
    }
    return {
      text: t(
        'export_advanced_summary_namespaces',
        '{count, plural, one {# namespace} other {# namespaces}}',
        { count: selected }
      ),
      state: 'changed',
    };
  }

  function getFormatOptionItems(): SummaryItem[] {
    const format = getFormatById(values.format);
    const result: SummaryItem[] = [];
    const messageFormat = normalizeSelectedMessageFormat(values);
    const defaultMessageFormat = normalizeSelectedMessageFormat({
      format: values.format,
      messageFormat: undefined,
    });
    if (
      format.supportedMessageFormats &&
      messageFormat &&
      messageFormat !== defaultMessageFormat
    ) {
      result.push({
        text: messageFormatTranslation[messageFormat],
        state: 'changed',
      });
    }
    if (
      format.showSupportArrays &&
      values.supportArrays !== (format.defaultSupportArrays ?? false)
    ) {
      result.push({
        text: values.supportArrays
          ? t('export_translations_support_arrays_label')
          : t('export_advanced_summary_no_support_arrays', 'Without arrays'),
        state: 'changed',
      });
    }
    if (format.showEscapeHtml && values.escapeHtml) {
      result.push({
        text: t('export_translations_escape_html_label'),
        state: 'changed',
      });
    }
    return result;
  }

  function getTagItems(): SummaryItem[] {
    if (!values.tagsIn.length && !values.tagsNotIn.length) {
      return [
        {
          text: t('export_advanced_summary_no_tags', 'No tag filters'),
          state: 'default',
        },
      ];
    }
    const result: SummaryItem[] = [];
    if (values.tagsIn.length) {
      result.push({
        text: t('export_advanced_summary_tags_in', 'Tags: {tags}', {
          tags: values.tagsIn.join(', '),
        }),
        state: 'changed',
      });
    }
    if (values.tagsNotIn.length) {
      result.push({
        text: t('export_advanced_summary_tags_not_in', 'Without tags: {tags}', {
          tags: values.tagsNotIn.join(', '),
        }),
        state: 'changed',
      });
    }
    return result;
  }
}
