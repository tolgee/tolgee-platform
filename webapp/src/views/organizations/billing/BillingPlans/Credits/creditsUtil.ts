import { components } from 'tg.service/billingApiSchema.generated';

type SliderValue = {
  totalAmount: number;
  totalPrice: number;
  priceId: number;
  itemQuantity: number;
  regularPrice: number | undefined;
};

export const getPossibleValues = (
  prices: components['schemas']['MtCreditsPriceModel'][]
): SliderValue[] | null => {
  const sortedPrices = prices.sort((a, b) => a.amount - b.amount);

  if (!sortedPrices) {
    return null;
  }

  let currentStep = 0;
  let stepAmount = 1;
  const lastStep = sortedPrices.length - 1;
  const max = sortedPrices[sortedPrices.length - 1].amount * 10;
  const result = [] as SliderValue[];

  const getCurrentPrice = () => {
    return stepAmount * sortedPrices[currentStep].price;
  };

  const getCurrentAmount = () => {
    return stepAmount * sortedPrices[currentStep].amount;
  };

  const getRegularPrice = () => {
    if (currentStep === 0) {
      return undefined;
    }
    const lowestTear = sortedPrices[0];
    const lowestTearUnitPrice = lowestTear.price / lowestTear.amount;
    return getCurrentAmount() * lowestTearUnitPrice;
  };

  while (getCurrentAmount() <= max) {
    if (
      lastStep > currentStep &&
      getCurrentPrice() >= sortedPrices[currentStep + 1].price
    ) {
      currentStep = currentStep + 1;
      stepAmount = 1;
    }
    result.push({
      totalAmount: getCurrentAmount(),
      totalPrice: getCurrentPrice(),
      priceId: sortedPrices[currentStep].id,
      itemQuantity: stepAmount,
      regularPrice: getRegularPrice(),
    });
    stepAmount++;
  }
  return result;
};
