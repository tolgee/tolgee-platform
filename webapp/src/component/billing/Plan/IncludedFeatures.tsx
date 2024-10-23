import { Box, SxProps, styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { useFeatures } from 'tg.translationTools/useFeatures';
import { PlanFeature } from 'tg.component/billing/PlanFeature';

type Features = components['schemas']['EeSubscriptionModel']['enabledFeatures'];

const StyledListWrapper = styled(Box)`
  display: flex;
  flex-direction: column;
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
  const featuresObject = useFeatures();

  return (
    <StyledListWrapper {...{ sx, className }}>
      {topFeature && <Box sx={{ marginBottom: '12px' }}>{topFeature}</Box>}
      {Object.entries(featuresObject)
        .filter(([key]) => features.includes(key as any))
        .map(([feature, translation]) => (
          <PlanFeature key={feature} name={translation} />
        ))}
    </StyledListWrapper>
  );
};
