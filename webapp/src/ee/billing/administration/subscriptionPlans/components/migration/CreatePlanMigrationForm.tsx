import { PlanMigrationForm } from './PlanMigrationForm';
import { components } from 'tg.service/billingApiSchema.generated';
import { PlanType } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/types';

export type CreatePlanMigrationFormData =
  components['schemas']['CreatePlanMigrationRequest'];

const emptyDefaultValues: CreatePlanMigrationFormData = {
  enabled: true,
  sourcePlanId: 0,
  targetPlanId: 0,
  monthlyOffsetDays: 14,
  yearlyOffsetDays: 30,
};

type Props = {
  onSubmit: (values: CreatePlanMigrationFormData) => void;
  loading?: boolean;
  planType?: PlanType;
};

export const CreatePlanMigrationForm: React.FC<Props> = (props) => {
  return (
    <PlanMigrationForm<CreatePlanMigrationFormData>
      defaultValues={emptyDefaultValues}
      {...props}
    />
  );
};
