import { Box, SxProps, styled } from '@mui/material';

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
  features: Features;
  topFeature?: React.ReactNode;
  sx?: SxProps;
  className?: string;
};

export const IncludedFeatures = ({
  features,
  topFeature,
  sx,
  className,
}: Props) => {
  const translateFeature = useFeatureTranslation();

  return (
    <StyledListWrapper {...{ sx, className }}>
      {topFeature && <Box sx={{ marginBottom: '12px' }}>{topFeature}</Box>}
      {features.map((feature) => (
        <PlanFeature key={feature} name={translateFeature(feature)} />
      ))}
    </StyledListWrapper>
  );
};
