import { useEffect, useState } from 'react';
import { CloudPlanFormData } from '../../../subscriptionPlans/components/planForm/cloud/types';
import { usePlanFormValues } from '../../../subscriptionPlans/components/planForm/cloud/usePlanFormValues';

export const useSetZeroPricesWhenFree = ({
  parentName,
}: {
  parentName?: string;
}) => {
  // Here we store the non-zero prices to be able to restore them when the plan is set back to non-free
  const [nonZeroPrices, setNonZeroPrices] =
    useState<CloudPlanFormData['prices']>();

  const { values, setFieldValue } =
    usePlanFormValues<CloudPlanFormData>(parentName);

  const free = values.free;
  const prices = values.prices;

  function setPriceValuesZero() {
    setNonZeroPrices(prices);
    const zeroPrices = Object.entries(prices).reduce(
      (acc, [key]) => ({ ...acc, [key]: 0 }),
      {}
    );

    setFieldValue(`prices`, zeroPrices);
  }

  function setPriceValuesNonZero() {
    setFieldValue(`prices`, nonZeroPrices);
  }

  useEffect(() => {
    if (free) {
      setPriceValuesZero();
      return;
    }
    if (nonZeroPrices) {
      setPriceValuesNonZero();
    }
  }, [free]);
};
