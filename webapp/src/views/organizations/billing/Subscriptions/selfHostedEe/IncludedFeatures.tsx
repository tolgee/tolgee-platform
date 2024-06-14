import { Box, SxProps, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { useFeatureTranslation } from 'tg.translationTools/useFeatureTranslation';
import { PlanFeature } from 'tg.component/billing/PlanFeature';

type Features = components['schemas']['EeSubscriptionModel']['enabledFeatures'];

const StyledListWrapper = styled(Box)`
  display: grid;
  margin-top: 8px;
  gap: 4px 8px;
  align-content: start;
`;

type Props = {
  allFromPlanName?: string | undefined;
  features: Features;
  sx?: SxProps;
  className?: string;
};

export const IncludedFeatures = ({
  allFromPlanName,
  features,
  sx,
  className,
}: Props) => {
  const translateFeature = useFeatureTranslation();

  return (
    <StyledListWrapper {...{ sx, className }}>
      {allFromPlanName && (
        <PlanFeature
          bold
          sx={{ marginBottom: '12px' }}
          name={
            <T
              keyName="billing_subscriptions_all_from_plan_label"
              params={{ name: allFromPlanName }}
            />
          }
        />
      )}
      {features.map((feature) => (
        <PlanFeature key={feature} name={translateFeature(feature)} />
      ))}
    </StyledListWrapper>
  );
};
