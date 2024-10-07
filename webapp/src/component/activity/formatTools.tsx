import { TextDiff } from './types/TextDiff';
import { AutoChange } from './types/AutoChange';
import { StateChange } from './types/StateChange';
import { DiffValue, FieldOptionsObj } from './types';
import { GeneralChange } from './types/GeneralChange';
import { CommentStateChange } from './types/CommentStateChange';
import { KeyTagsChange } from './types/KeyTagsChange';
import { LanguageFlagChange } from './types/LanguageFlagChange';
import { ProjectLanguageChange } from './types/ProjectLanguageChange';
import { NoDiffChange } from './types/NoDiffChange';
import { NamespaceChange } from './types/NamespaceChange';
import { OutdatedChange } from './types/OutdatedChange';
import { BatchLanguageIdsChange } from './types/BatchLanguageIdsChange';
import { BatchLanguageIdChange } from './types/BatchLanguageIdChange';
import { BatchKeyTagListChange } from './types/BatchKeyTagListChange';
import { BatchNamespaceChange } from './types/BatchNamespaceChange';
import { BatchStateChange } from './types/BatchStateChange';
import { DefaultNamespaceChange } from './types/DefaultNamespaceChange';
import { DateChange } from './types/DateChange';
import { TaskStateChange } from './types/TaskStateChange';
import { TaskTypeChange } from './types/TaskTypeChange';

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
      return <AutoChange input={value} />;
    case 'translation_state':
      return <StateChange input={value} />;
    case 'comment_state':
      return <CommentStateChange input={value} />;
    case 'key_tags':
      return <KeyTagsChange input={value} />;
    case 'text':
      return diffEnabled ? (
        <TextDiff input={value} languageTag={languageTag} />
      ) : (
        <NoDiffChange input={value} languageTag={languageTag} />
      );
    case 'language_flag':
      return <LanguageFlagChange input={value} />;
    case 'project_language':
      return <ProjectLanguageChange input={value} />;
    case 'namespace':
      return <NamespaceChange input={value} diffEnabled={diffEnabled} />;
    case 'outdated':
      return <OutdatedChange input={value} />;
    case 'batch_language_ids':
      return <BatchLanguageIdsChange input={value} />;
    case 'batch_language_id':
      return <BatchLanguageIdChange input={value} />;
    case 'batch_key_tag_list':
      return <BatchKeyTagListChange input={value} />;
    case 'batch_namespace':
      return <BatchNamespaceChange input={value} />;
    case 'batch_translation_state':
      return <BatchStateChange input={value} />;
    case 'default_namespace':
      return <DefaultNamespaceChange input={value} />;
    case 'date':
      return <DateChange input={value} />;
    case 'task_state':
      return <TaskStateChange input={value} />;
    case 'task_type':
      return <TaskTypeChange input={value} />;
    default:
      return diffEnabled ? (
        <GeneralChange input={value} />
      ) : (
        <NoDiffChange input={value} />
      );
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
