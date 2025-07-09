import { StandardForm } from 'tg.component/common/form/StandardForm';
import { components } from 'tg.service/apiSchema.generated';
import { FC } from 'react';
import { Box } from '@mui/material';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { FieldLabel } from 'tg.component/FormField';
import { T, useTranslate } from '@tolgee/react';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { ColorPaletteField } from 'tg.component/common/form/fields/ColorPaletteField';
import { tolgeePalette } from '../../../figmaTheme';

type LabelModel = components['schemas']['LabelModel'];

export type LabelFormValues = {
  name: string;
  color: string;
  description: string | undefined;
};

export const LabelForm: FC<{
  label?: LabelModel;
  submit: (values: LabelFormValues) => void;
  cancel?: () => void;
  submitText?: string;
}> = ({ label, submit, cancel, submitText }) => {
  const { t } = useTranslate();
  const initValues = {
    name: label?.name ?? '',
    description: label?.description,
    color: label?.color ?? '',
  } satisfies LabelFormValues;
  const onSubmit = (values: LabelFormValues) => {
    submit({
      ...values,
      color: values.color.toUpperCase(),
    });
  };

  return (
    <StandardForm
      validationSchema={Validation.TRANSLATION_LABEL(t)}
      initialValues={initValues}
      onSubmit={onSubmit}
      onCancel={cancel}
      submitButtonInner={submitText}
    >
      <Box mb={4}>
        <Box display="flex" gap={2} mb={2}>
          <Box display="grid" flexGrow={1}>
            <FieldLabel>
              <T keyName="project_settings_label_name" />
            </FieldLabel>
            <TextField size="small" name="name" required={true} />
          </Box>
          <Box display="grid">
            <FieldLabel>
              <T keyName="project_settings_label_color" />
            </FieldLabel>
            <ColorPaletteField
              name="color"
              colors={tolgeePalette.Light.label}
              darkColors={tolgeePalette.Dark.label}
              required={true}
            />
          </Box>
        </Box>
        <Box display="grid">
          <FieldLabel>
            <T keyName="project_settings_label_description" />
          </FieldLabel>
          <TextField
            size="medium"
            multiline
            name="description"
            required={false}
          />
        </Box>
      </Box>
    </StandardForm>
  );
};
