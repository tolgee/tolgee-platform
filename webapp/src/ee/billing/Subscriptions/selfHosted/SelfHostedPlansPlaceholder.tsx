import { Box, Button, styled, Typography } from '@mui/material';
import { T } from '@tolgee/react';

import { SelfHostedPlaceholder } from 'tg.component/CustomIcons';

const StyledContainer = styled(Box)`
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 700px;
  margin: 0 auto;
  padding: ${({ theme }) => theme.spacing(6, 4)};
  background: ${({ theme }) => theme.palette.tokens.background.hover};
  border-radius: 20px;
`;

const StyledIcon = styled(SelfHostedPlaceholder)`
  color: ${({ theme }) => theme.palette.tokens.icon.secondary};
  margin-top: ${({ theme }) => theme.spacing(2)};
`;

export const SelfHostedPlansPlaceholder = () => {
  return (
    <StyledContainer data-cy="self-hosted-plans-placeholder">
      <Typography variant="h5" mb={1}>
        <T
          keyName="self_hosted_plans_placeholder_title"
          defaultValue="Self-hosted plan"
        />
      </Typography>
      <Typography
        variant="body1"
        color={(theme) => theme.palette.text.secondary}
        mb={3}
        textAlign="center"
      >
        <T
          keyName="self_hosted_plans_placeholder_description"
          defaultValue="Self-hosted plans are available on request. Contact us for a custom offer."
        />
      </Typography>
      <Button
        variant="contained"
        color="primary"
        href="mailto:info@tolgee.io"
        data-cy="self-hosted-plans-placeholder-contact-us"
      >
        <T
          keyName="self_hosted_plans_placeholder_contact_us"
          defaultValue="Contact us"
        />
      </Button>
      <StyledIcon />
    </StyledContainer>
  );
};
