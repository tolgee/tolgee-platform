import { T, useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';
import { BaseLanguageSelect } from 'tg.component/languages/BaseLanguageSelect';
import { LabelHint } from 'tg.component/common/LabelHint';
import { CreateEditTranslationMemoryFormValues } from 'tg.ee.module/translationMemory/components/form/TranslationMemoryCreateEditForm';

type Props = {
  disabled: boolean;
};

/**
 * Base-language picker for the TM create/edit form. Locks when there are any project
 * assignments — base language must match every assigned project's base, so we cannot
 * change it once the choice has been concretized by an assignment.
 */
export const BaseLanguageFieldWithHint = ({ disabled }: Props) => {
  const { t } = useTranslate();
  const { values } = useFormikContext<CreateEditTranslationMemoryFormValues>();
  const hasAssignedProjects = values.assignedProjects.length > 0;
  return (
    <BaseLanguageSelect
      name="baseLanguage"
      disabled={disabled || hasAssignedProjects}
      minHeight={false}
      autoSelectFirst={false}
      label={
        <LabelHint
          title={
            hasAssignedProjects ? (
              <T
                keyName="translation_memory_settings_base_language_locked_hint"
                defaultValue="Base language is locked while projects are assigned. Remove all projects first to change it."
              />
            ) : (
              <T
                keyName="translation_memory_settings_base_language_hint"
                defaultValue="Must be the same across all projects using this TM."
              />
            )
          }
        >
          {t('field_base_language', 'Base language')}
        </LabelHint>
      }
    />
  );
};
