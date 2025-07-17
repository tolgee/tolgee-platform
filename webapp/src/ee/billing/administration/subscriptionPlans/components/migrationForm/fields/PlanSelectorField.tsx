import { useFormikContext } from 'formik';
import { CloudPlanSelector } from 'tg.ee.module/billing/administration/subscriptionPlans/components/planForm/cloud/fields/CloudPlanSelector';
import { PlanMigrationFormData } from 'tg.ee.module/billing/administration/subscriptionPlans/components/migrationForm/PlanMigrationForm';
import { GenericPlanSelector } from 'tg.ee.module/billing/administration/subscriptionPlans/components/planForm/genericFields/GenericPlanSelector';

export const PlanSelectorField = ({
  name,
  filterHasMigration,
  ...props
}: { name: string; filterHasMigration?: boolean } & Omit<
  GenericPlanSelector<any>,
  'onChange'
>) => {
  const { setFieldValue, values } = useFormikContext<PlanMigrationFormData>();

  return (
    <CloudPlanSelector
      {...props}
      filterHasMigration={filterHasMigration}
      value={values[name]}
      onChange={(value) => setFieldValue(name, value)}
    />
  );
};
