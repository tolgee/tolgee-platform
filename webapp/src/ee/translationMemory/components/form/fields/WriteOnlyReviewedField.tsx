import { useFormikContext } from 'formik';
import { CreateEditTranslationMemoryFormValues } from 'tg.ee.module/translationMemory/components/form/TranslationMemoryCreateEditForm';
import { WriteOnlyReviewedSwitch } from 'tg.ee.module/translationMemory/components/form/fields/WriteOnlyReviewedSwitch';

type Props = {
  disabled: boolean;
};

/**
 * Formik adapter around [WriteOnlyReviewedSwitch] — bridges the controlled switch to the
 * `writeOnlyReviewed` field in the surrounding TM create/edit form.
 */
export const WriteOnlyReviewedField = ({ disabled }: Props) => {
  const { values, setFieldValue } =
    useFormikContext<CreateEditTranslationMemoryFormValues>();
  return (
    <WriteOnlyReviewedSwitch
      checked={values.writeOnlyReviewed}
      onChange={(v) => setFieldValue('writeOnlyReviewed', v)}
      disabled={disabled}
      switchDataCy="tm-settings-write-only-reviewed"
    />
  );
};
