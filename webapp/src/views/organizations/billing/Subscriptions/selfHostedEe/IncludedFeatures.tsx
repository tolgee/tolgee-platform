import { FC } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { T } from '@tolgee/react';
import { Box, styled, Typography } from '@mui/material';
import { PlanFeature } from './PlanFeature';
import { useFeatureTranslation } from 'tg.translationTools/useFeatureTranslation';

const StyledListWrapper = styled(Box)`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  margin-top: 8px;
  gap: 4px 8px;
`;

interface IncludedProps {
  included: components['schemas']['PlanIncludedUsageModel'];
}

function Included(props: IncludedProps) {
  return (
    <>
      {props.included.seats > 0 && (
        <Box>
          <T
            keyName="billinb_self_hosted_plan_included_seats"
            params={{ seats: props.included.seats }}
          />
        </Box>
      )}
      {props.included.seats == -1 && (
        <Box>
          <T keyName="billinb_self_hosted_plan_unlimited_seats" />
        </Box>
      )}
      {props.included.mtCredits > 0 && (
        <Box>
          <T
            keyName="billinb_self_hosted_plan_included_mtCredits"
            params={{ mtCredits: props.included.mtCredits }}
          />
        </Box>
      )}
    </>
  );
}

export const IncludedFeatures: FC<{
  features: components['schemas']['EeSubscriptionModel']['enabledFeatures'];
  includedUsage?: components['schemas']['PlanIncludedUsageModel'];
}> = ({ features, includedUsage }) => {
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
        {includedUsage && <Included included={includedUsage} />}
        {features.map((feature) => (
          <PlanFeature key={feature} name={translateFeature(feature)} />
        ))}
      </StyledListWrapper>
    </Box>
  );
};
