import { FC } from 'react';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';
import {
  CreatePlanMigrationFormData,
  EmailTemplateData,
  PlanMigrationFormData,
} from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/PlanMigrationForm';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Box,
  Typography,
} from '@mui/material';
import { HtmlTemplateEditor } from 'tg.component/common/form/HtmlTemplateEditor';
import { ArrowDropDown } from 'tg.component/CustomIcons';

type EmailSectionProps = {
  template?: EmailTemplateData;
};

export const PlanMigrationEmailSection: FC<EmailSectionProps> = ({
  template,
}) => {
  const { t } = useTranslate();
  const { values, setFieldValue } = useFormikContext<
    CreatePlanMigrationFormData | PlanMigrationFormData
  >();

  return (
    <Accordion
      sx={{ mt: 1 }}
      defaultExpanded={false}
      data-cy="plan-migration-email-section"
    >
      <AccordionSummary expandIcon={<ArrowDropDown />}>
        <Typography>
          {t('administration_plan_migration_email_section_title')}
        </Typography>
      </AccordionSummary>
      <AccordionDetails>
        <Box mt={1}>
          <HtmlTemplateEditor
            name="emailTemplate"
            value={values.customEmailBody ?? template?.body ?? ''}
            onChange={(val) => setFieldValue('customEmailBody', val)}
            disabled={!template}
            placeholders={template?.placeholders ?? []}
          />
        </Box>
      </AccordionDetails>
    </Accordion>
  );
};
