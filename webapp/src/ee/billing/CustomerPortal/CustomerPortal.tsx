import { Box, Button, styled, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import {
  StyledBillingSection,
  StyledBillingSectionHeader,
  StyledBillingSectionTitle,
} from '../BillingSection';
import StripeLogoSvg from 'tg.svgs/stripeLogo.svg?react';
import { useGoToStripeCustomerPortal } from '../Subscriptions/cloud/useGoToStripeCustomerPortal';

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
  const goToCustomerPortal = useGoToStripeCustomerPortal();

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
            onClick={goToCustomerPortal}
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
