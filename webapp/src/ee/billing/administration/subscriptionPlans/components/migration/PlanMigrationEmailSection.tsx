import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';
import {
  CreatePlanMigrationFormData,
  EmailTemplateData,
  PlanMigrationFormData,
} from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/PlanMigrationForm';
import { Box, Typography } from '@mui/material';
import { HtmlTemplateEditor } from 'tg.component/common/form/HtmlTemplateEditor';

type EmailSectionProps = {
  template?: EmailTemplateData;
};

export const PlanMigrationEmailSection = ({ template }: EmailSectionProps) => {
  const { t } = useTranslate();
  const { values, setFieldValue } = useFormikContext<
    CreatePlanMigrationFormData | PlanMigrationFormData
  >();

  return (
    <Box mt={1} display="grid" gap={1}>
      <Typography>
        {t('administration_plan_migration_email_section_title')}
      </Typography>
      <HtmlTemplateEditor
        value={values.customEmailBody ?? template?.body ?? ''}
        onChange={(val) => setFieldValue('customEmailBody', val)}
        disabled={!template}
        placeholders={template?.placeholders ?? []}
      />
    </Box>
  );
};
