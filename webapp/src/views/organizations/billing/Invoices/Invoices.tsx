import { FC, useState } from 'react';
import { Box, styled } from '@mui/material';

import { useOrganization } from 'tg.views/organizations/useOrganization';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { useDateFormatter, useMoneyFormatter } from 'tg.hooks/useLocale';
import { DownloadButton } from './DownloadButton';
import { useTranslate } from '@tolgee/react';
import {
  StyledBillingSection,
  StyledBillingSectionHeader,
  StyledBillingSectionTitle,
} from '../BillingSection';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { InvoiceUsage } from './InvoiceUsage';

const StyledGrid = styled('div')`
  display: grid;
  grid-template-columns: 1fr 1fr 1fr auto auto;
`;

const StyledContainer = styled('div')`
  display: grid;
  gap: 20px;
  padding-top: 10px;
  align-items: center;
`;

const StyledItem = styled(Box)`
  display: grid;
  align-self: center;
  padding: 4px 0px;
`;

export const Invoices: FC = () => {
  const organization = useOrganization();
  const { t } = useTranslate();

  const formatPrice = useMoneyFormatter();
  const formatDate = useDateFormatter();

  const [page, setPage] = useState(0);

  const invoicesLoadable = useBillingApiQuery({
    url: '/v2/organizations/{organizationId}/billing/invoices',
    method: 'get',
    query: {
      page: page,
      size: 20,
    },
    path: {
      organizationId: organization!.id,
    },
    options: {
      keepPreviousData: true,
    },
  });

  return (
    <StyledBillingSection gridArea="invoices">
      <StyledBillingSectionTitle>
        <StyledBillingSectionHeader>
          <span>{t('billing_customer_invoices_title')}</span>
        </StyledBillingSectionHeader>
      </StyledBillingSectionTitle>
      <StyledContainer data-cy="billing-invoices-list">
        <PaginatedHateoasList
          onPageChange={(p) => setPage(p)}
          listComponent={StyledGrid}
          wrapperComponent={'div'}
          emptyPlaceholder={
            <EmptyListMessage loading={invoicesLoadable.isLoading}>
              {t('billing_invoices_empty')}
            </EmptyListMessage>
          }
          renderItem={(item) => (
            <>
              <StyledItem data-cy="billing-invoice-item-number">
                {item.number}
              </StyledItem>
              <StyledItem>{formatDate(item.createdAt)}</StyledItem>
              <StyledItem>{formatPrice(item.total)}</StyledItem>
              <StyledItem>
                <Box>
                  <DownloadButton invoice={item} />
                </Box>
              </StyledItem>
              <StyledItem>
                <InvoiceUsage invoice={item} />
              </StyledItem>
            </>
          )}
          loadable={invoicesLoadable}
        />
      </StyledContainer>
    </StyledBillingSection>
  );
};
