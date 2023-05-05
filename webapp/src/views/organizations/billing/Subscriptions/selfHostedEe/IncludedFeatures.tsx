import { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { T, useTranslate } from '@tolgee/react';
import { Box, Typography, styled } from '@mui/material';
import { PlanFeature } from './PlanFeature';
import { useFeatureTranslation } from 'tg.translationTools/useFeatureTranslation';

const StyledListWrapper = styled(Box)`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  margin-top: 8px;
  gap: 4px 8px;
`;

export const IncludedFeatures: FC<{
  features: components['schemas']['SelfHostedEePlanModel']['enabledFeatures'];
}> = ({ features }) => {
  const translateFeature = useFeatureTranslation();

  return (
    <Box>
      <Typography
        mt={1}
        mb={2}
        sx={{ fontSize: '12px', fontStyle: 'italic' }}
        color="primary"
      >
        <T keyName="billing_subscriptions_plan_includes_title" />
      </Typography>
      <StyledListWrapper>
        {features.map((feature) => (
          <PlanFeature key={feature} name={translateFeature(feature)} />
        ))}
      </StyledListWrapper>
    </Box>
  );
};
