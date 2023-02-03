import { getTextDiff } from './types/getTextDiff';
import { getAutoChange } from './types/getAutoChange';
import { getStateChange } from './types/getStateChange';
import { DiffValue, FieldOptionsObj } from './types';
import { getGeneralChange } from './types/getGeneralChange';
import { getCommentStateChange } from './types/getCommentStateChange';
import { getKeyTagsChange } from './types/getKeyTagsChange';
import { getLanguageFlagChange } from './types/getLanguageFlagChange';
import { getProjectLanguageChange } from './types/getProjectLanguageChange';
import { getNoDiffChange } from './types/getNoDiffChange';
import { getNamespaceChange } from './types/getNamespaceChange';
import { getOutdatedChange } from './types/getOutdatedChange';

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
    default:
      return diffEnabled ? getGeneralChange(value) : getNoDiffChange(value);
  }
};
