import { FC, useEffect, useState } from 'react';
import { usePlanFormValues } from '../usePlanFormValues';
import { PlanPricesFields } from '../../genericFields/PlanPricesFields';
import { CloudPlanFormData } from '../types';

type CloudPlanPricesProps = {
  parentName: string | undefined;
};

export const CloudPlanPrices: FC<CloudPlanPricesProps> = ({ parentName }) => {
  // Here we store the non-zero prices to be able to restore them when the plan is set back to non-free
  const [nonZeroPrices, setNonZeroPrices] =
    useState<CloudPlanFormData['prices']>();

  const { values, setFieldValue } =
    usePlanFormValues<CloudPlanFormData>(parentName);

  const free = values.free;
  const prices = values.prices;
  const type = values.type;
  const metricType = values.metricType;

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

  if (free) {
    return null;
  }

  return (
    <PlanPricesFields
      isPayAsYouGo={type !== 'PAY_AS_YOU_GO'}
      parentName={parentName}
      metricType={metricType}
    />
  );
};
