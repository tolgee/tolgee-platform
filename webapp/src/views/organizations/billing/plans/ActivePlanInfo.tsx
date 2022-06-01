import { Box } from '@mui/material';
import { useCurrentLanguage } from '@tolgee/react';
import { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';

type ActivePlanModel = components['schemas']['ActivePlanModel'];

type Props = {
  plan: ActivePlanModel;
};

export const ActivePlanInfo: FC<Props> = ({ plan }) => {
  const getCurrentLang = useCurrentLanguage();

  return (
    <>
      <Box>This is Active</Box>
      <Box>
        Period end:{' '}
        {plan.currentPeriodEnd
          ? new Date(plan.currentPeriodEnd).toLocaleDateString(getCurrentLang())
          : '-'}
      </Box>
      <Box>
        Cancel at period end: {plan.cancelAtPeriodEnd ? 'true' : 'false'}
      </Box>{' '}
    </>
  );
};
