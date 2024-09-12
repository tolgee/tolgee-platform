import { Box, Button, styled, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import {
  StyledBillingSection,
  StyledBillingSectionHeader,
  StyledBillingSectionTitle,
} from '../BillingSection';
import StripeLogoSvg from 'tg.svgs/stripeLogo.svg?react';

const StyledContainer = styled('div')`
  display: grid;
  gap: 10px;
  padding-top: 10px;
`;

const StyledStripeLogo = styled(StripeLogoSvg)`
  color: ${({ theme }) => theme.palette.text.secondary};
  height: 20px;
`;

export const CustomerPortal = () => {
  const { t } = useTranslate();
  const organization = useOrganization();

  const getCustomerPortalSession = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/customer-portal',
    method: 'get',
    options: {
      onSuccess: (data) => {
        window.location.href = data.url;
      },
    },
  });

  return (
    <StyledBillingSection gridArea="customerPortal">
      <StyledBillingSectionTitle>
        <StyledBillingSectionHeader>
          <Box display="flex" alignItems="center" gap={2}>
            <span>{t('billing_customer_portal_title')}</span>
            <StyledStripeLogo />
          </Box>
          <Button
            size="small"
            color="primary"
            variant="contained"
            onClick={() =>
              getCustomerPortalSession.mutate({
                path: {
                  organizationId: organization!.id,
                },
              })
            }
          >
            {t('billing_customer_portal_button')}
          </Button>
        </StyledBillingSectionHeader>
      </StyledBillingSectionTitle>
      <StyledContainer>
        <Typography>{t('billing_customer_portal_info')}</Typography>
      </StyledContainer>
    </StyledBillingSection>
  );
};
