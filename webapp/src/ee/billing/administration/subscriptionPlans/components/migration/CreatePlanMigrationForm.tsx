import {
  CreatePlanMigrationFormData,
  EmailTemplateData,
  PlanMigrationForm,
} from './PlanMigrationForm';
import { PlanType } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/types';

const emptyDefaultValues: CreatePlanMigrationFormData = {
  enabled: true,
  sourcePlanId: 0,
  targetPlanId: 0,
  monthlyOffsetDays: 14,
  yearlyOffsetDays: 30,
  customEmailBody: undefined,
};

type Props = {
  onSubmit: (values: CreatePlanMigrationFormData) => void;
  loading?: boolean;
  planType?: PlanType;
  emailTemplate?: EmailTemplateData;
};

export const CreatePlanMigrationForm: React.FC<Props> = (props) => {
  return (
    <PlanMigrationForm<CreatePlanMigrationFormData>
      defaultValues={emptyDefaultValues}
      {...props}
    />
  );
};
