import { Box, styled, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import {
  OrganizationSwitch,
  SwitchSurface,
} from 'tg.component/organizationSwitch/OrganizationSwitch';

type OrganizationModel = components['schemas']['OrganizationModel'];

const StyledWrapper = styled(Box)`
  display: flex;
  align-items: center;
  gap: 12px;
`;

const StyledTitle = styled(Typography)`
  font-weight: 600;
`;

type Props = {
  communityNavigation?: boolean;
  selectedSurface?: SwitchSurface;
  onSelect?: (organization: OrganizationModel) => void;
};

export const ProjectsListTitle: React.FC<Props> = ({
  communityNavigation,
  selectedSurface,
  onSelect,
}) => {
  const { t } = useTranslate();
  return (
    <StyledWrapper>
      <StyledTitle variant="h5" data-cy="projects-list-title">
        {t('projects_title')}
      </StyledTitle>
      <OrganizationSwitch
        communityNavigation={communityNavigation}
        selectedSurface={selectedSurface}
        onSelect={onSelect}
      />
    </StyledWrapper>
  );
};
