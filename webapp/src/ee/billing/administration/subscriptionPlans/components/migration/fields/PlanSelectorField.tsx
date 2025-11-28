import { useFormikContext } from 'formik';
import { CloudPlanSelector } from 'tg.ee.module/billing/administration/subscriptionPlans/components/planForm/cloud/fields/CloudPlanSelector';
import {
  CreatePlanMigrationFormData,
  PlanMigrationFormData,
} from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/PlanMigrationForm';
import { GenericPlanSelector } from 'tg.ee.module/billing/administration/subscriptionPlans/components/planForm/genericFields/GenericPlanSelector';
import { SelfHostedEePlanSelector } from 'tg.ee.module/billing/administration/subscriptionPlans/components/planForm/selfHostedEe/fields/SelfHostedEePlanSelector';
import { PlanType } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migration/types';

export const PlanSelectorField = ({
  name,
  type = 'cloud',
  filterHasMigration,
  ...props
}: {
  name: string;
  type?: PlanType;
  filterHasMigration?: boolean;
} & Omit<GenericPlanSelector<any>, 'onChange'>) => {
  const { setFieldValue, values } = useFormikContext<
    PlanMigrationFormData | CreatePlanMigrationFormData
  >();

  const Selector =
    type === 'cloud' ? CloudPlanSelector : SelfHostedEePlanSelector;

  return (
    <Selector
      {...props}
      filterHasMigration={filterHasMigration}
      value={values[name]}
      onChange={(value) => setFieldValue(name, value)}
    />
  );
};
