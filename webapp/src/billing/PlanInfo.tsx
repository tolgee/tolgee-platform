import { Box, Typography } from '@mui/material';
import { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';

export const PlanInfo: FC<{ plan: components['schemas']['PlanModel'] }> = ({
  plan,
}) => {
  return (
    <>
      <Typography variant="h2">{plan.name}</Typography>
      <Box>Translation limit: {plan.translationLimit}</Box>
      <Box>Mt Credits included: {(plan.includedMtCredits || 0) / 100}</Box>
    </>
  );
};
