import { PlanMigrationForm, PlanMigrationFormData } from './PlanMigrationForm';
import { components } from 'tg.service/billingApiSchema.generated';
import { PlanType } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/types';

type CloudPlanMigrationModel = components['schemas']['CloudPlanMigrationModel'];
type SelfHostedEePlanMigrationModel =
  components['schemas']['AdministrationSelfHostedEePlanMigrationModel'];

type Props = {
  onSubmit: (values: PlanMigrationFormData) => void;
  loading?: boolean;
  onDelete?: (id: number) => void;
  migration: CloudPlanMigrationModel | SelfHostedEePlanMigrationModel;
  planType?: PlanType;
};

export const EditPlanMigrationForm: React.FC<Props> = (props) => {
  const { migration } = props;
  const initialValues: PlanMigrationFormData = {
    enabled: migration.enabled,
    targetPlanId: migration.targetPlan.id,
    monthlyOffsetDays: migration.monthlyOffsetDays,
    yearlyOffsetDays: migration.yearlyOffsetDays,
  };
  return (
    <PlanMigrationForm<PlanMigrationFormData>
      defaultValues={initialValues}
      {...props}
    />
  );
};
