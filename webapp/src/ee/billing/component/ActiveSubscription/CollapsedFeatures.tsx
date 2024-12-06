import { styled } from '@mui/material';
import { IncludedFeatures } from '../Plan/IncludedFeatures';
import { components } from 'tg.service/apiSchema.generated';
import { useState } from 'react';
import {
  ShowAllFeaturesButton,
  ShowAllFeaturesLink,
} from '../Plan/ShowAllFeatures';
import { PlanFeaturesBox } from '../Plan/PlanStyles';

type Features = components['schemas']['EeSubscriptionModel']['enabledFeatures'];

const StyledFeatures = styled(IncludedFeatures)`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(200px, 100%), 1fr));
  margin: 0px;
`;

type Props = {
  custom: boolean;
  features: Features;
};

export const CollapsedFeatures = ({ custom, features }: Props) => {
  const [expanded, setExpanded] = useState(false);
  return (
    <>
      {custom && expanded && (
        <PlanFeaturesBox sx={{ gap: '18px', mb: 1 }}>
          <StyledFeatures features={features} />
        </PlanFeaturesBox>
      )}
      {custom && !expanded && (
        <ShowAllFeaturesButton onClick={() => setExpanded(true)} />
      )}
      {!custom && <ShowAllFeaturesLink />}
    </>
  );
};
