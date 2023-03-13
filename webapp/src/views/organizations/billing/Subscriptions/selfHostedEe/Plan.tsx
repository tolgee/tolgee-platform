import { components } from 'tg.service/billingApiSchema.generated';
import { StyledContent, StyledPlan } from '../cloud/Plans/StyledPlan';
import { PlanTitle } from '../cloud/Plans/PlanTitle';
import { PlanActionButton } from '../cloud/Plans/PlanActionButton';
import { T, useTranslate } from '@tolgee/react';
import { PlanPrice } from '../cloud/Plans/PlanPrice';
import { Box } from '@mui/material';
import { ReactNode } from 'react';

export const Plan = (props: {
  planModel: components['schemas']['SelfHostedEePlanModel'];
}) => {
  const { t } = useTranslate();

  const description =
    props.planModel.subscriptionPrice == 0
      ? t('billing_subscriptions_pay_for_what_you_use')
      : t('billing_subscriptions_pay_fixed_price', {
          includedSeats: props.planModel.includedSeats,
        });

  const featuresMap: Record<
    components['schemas']['SelfHostedEePlanModel']['enabledFeatures'][0],
    ReactNode
  > = {
    GRANULAR_PERMISSIONS: t(
      'billing_subscriptions_granular_permissions_feature'
    ),
  };

  return (
    <>
      <StyledPlan>
        <StyledContent>
          <PlanTitle title={props.planModel.name}></PlanTitle>

          <Box>
            {description}
            <Box>
              <Box mt={1}>
                <T keyName="billing_subscriptions_plan_includes_title" />
              </Box>
              <ul>
                {props.planModel.enabledFeatures.map((feature) => (
                  <li key={feature}>{featuresMap[feature]}</li>
                ))}
              </ul>
            </Box>
          </Box>

          <Box gridArea="action" display="grid">
            <Box
              display="flex"
              justifyContent="space-between"
              alignItems="center"
            >
              <PlanPrice
                pricePerSeat={props.planModel.pricePerSeat}
                subscriptionPrice={props.planModel.subscriptionPrice}
              />
              <PlanActionButton>{t('billing_plan_subscribe')}</PlanActionButton>
            </Box>
          </Box>
        </StyledContent>
      </StyledPlan>
    </>
  );
};
