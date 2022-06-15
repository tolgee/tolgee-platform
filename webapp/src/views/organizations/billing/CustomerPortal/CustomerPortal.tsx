import { Button, styled, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { BillingSection } from '../BillingSection';
import { Invoices } from './Invoices';

const StyledHeader = styled('div')`
  display: flex;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
`;

const StyledContainer = styled('div')`
  display: grid;
  gap: 10px;
  padding-top: 10px;
`;

export const CustomerPortal = () => {
  const t = useTranslate();
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
    <BillingSection
      title={
        <StyledHeader>
          <span>{t('billing_customer_portal_title')}</span>
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
        </StyledHeader>
      }
    >
      <StyledContainer>
        <Typography>{t('billing_customer_portal_info')}</Typography>
        <Invoices />
      </StyledContainer>
    </BillingSection>
  );
};
