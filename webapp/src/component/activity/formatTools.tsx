import { DiffValue, FieldOptionsObj } from './types';
import { getTextDiff } from './types/getTextDiff';
import { getAutoChange } from './types/getAutoChange';
import { getStateChange } from './types/getStateChange';
import { getGeneralChange } from './types/getGeneralChange';
import { getCommentStateChange } from './types/getCommentStateChange';
import { getKeyTagsChange } from './types/getKeyTagsChange';
import { getLanguageFlagChange } from './types/getLanguageFlagChange';
import { getProjectLanguageChange } from './types/getProjectLanguageChange';
import { getNoDiffChange } from './types/getNoDiffChange';
import { getNamespaceChange } from './types/getNamespaceChange';
import { getOutdatedChange } from './types/getOutdatedChange';
import { getBatchLanguageIdsChange } from './types/getBatchLanguageIdsChange';
import { getBatchLanguageIdChange } from './types/getBatchLanguageIdChange';
import { getBatchKeyTagListChange } from './types/getBatchKeyTagListChange';
import { getBatchNamespaceChange } from './types/getBatchNamespaceChange';
import { getBatchStateChange } from './types/getBatchStateChange';
import { getDefaultNamespaceChange } from './types/getDefaultNamespaceChange';
import { getDateChange } from './types/getDateChange';
import { getTaskStateChange } from './types/getTaskStateChange';
import { getTaskTypeChange } from './types/getTaskTypeChange';

type Props = {
  value: DiffValue<any>;
  options: FieldOptionsObj;
  diffEnabled: boolean;
  languageTag?: string | undefined;
};

export const formatDiff = ({
  value,
  options,
  diffEnabled,
  languageTag,
}: Props) => {
  switch (options.type) {
    case 'translation_auto':
      return getAutoChange(value);
    case 'translation_state':
      return getStateChange(value);
    case 'comment_state':
      return getCommentStateChange(value);
    case 'key_tags':
      return getKeyTagsChange(value);
    case 'text':
      return diffEnabled
        ? getTextDiff(value, languageTag)
        : getNoDiffChange(value, languageTag);
    case 'language_flag':
      return getLanguageFlagChange(value);
    case 'project_language':
      return getProjectLanguageChange(value);
    case 'namespace':
      return getNamespaceChange(value, diffEnabled);
    case 'outdated':
      return getOutdatedChange(value);
    case 'batch_language_ids':
      return getBatchLanguageIdsChange(value);
    case 'batch_language_id':
      return getBatchLanguageIdChange(value);
    case 'batch_key_tag_list':
      return getBatchKeyTagListChange(value);
    case 'batch_namespace':
      return getBatchNamespaceChange(value);
    case 'batch_translation_state':
      return getBatchStateChange(value);
    case 'default_namespace':
      return getDefaultNamespaceChange(value);
    case 'date':
      return getDateChange(value);
    case 'task_state':
      return getTaskStateChange(value);
    case 'task_type':
      return getTaskTypeChange(value);
    default:
      return diffEnabled ? getGeneralChange(value) : getNoDiffChange(value);
  }
};

export function valueToText(value: any) {
  if (
    typeof value === 'string' ||
    typeof value === 'number' ||
    typeof value === 'boolean' ||
    value === null ||
    value === undefined
  ) {
    return value;
  }
  if (Array.isArray(value)) {
    return value.join(', ');
  }
  if (typeof value === 'object') {
    return JSON.stringify(value);
  }
}
