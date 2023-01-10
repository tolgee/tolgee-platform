import { FC, useState } from 'react';
import { Box, Button, styled } from '@mui/material';

import { useOrganization } from 'tg.views/organizations/useOrganization';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { useDateFormatter, useMoneyFormatter } from 'tg.hooks/useLocale';
import { DownloadButton } from './DownloadButton';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { useTranslate } from '@tolgee/react';
import {
  StyledBillingSection,
  StyledBillingSectionHeader,
  StyledBillingSectionTitle,
} from '../BillingSection';
import { InvoicesModal } from './InvoicesModal';

const StyledGrid = styled('div')`
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  max-width: 500px;
`;

const StyledContainer = styled('div')`
  display: grid;
  gap: 10px;
  padding-top: 10px;
  align-items: center;
`;

const StyledItem = styled(Box)`
  display: grid;
  align-self: center;
  padding: 4px 0px;
`;

const ITEMS_COUNT = 3;

export const Invoices: FC = () => {
  const organization = useOrganization();
  const { t } = useTranslate();

  const [showAll, setShowAll] = useState(false);

  const formatPrice = useMoneyFormatter();
  const formatDate = useDateFormatter();

  const invoicesLoadable = useBillingApiQuery({
    url: '/v2/organizations/{organizationId}/billing/invoices/',
    method: 'get',
    query: {
      size: 3,
    },
    path: {
      organizationId: organization!.id,
    },
    options: {
      keepPreviousData: true,
    },
  });

  const showAllButton =
    (invoicesLoadable.data?.page?.totalElements || 0) > ITEMS_COUNT;

  useGlobalLoading(invoicesLoadable.isFetching);

  return (
    <StyledBillingSection gridArea="invoices">
      <StyledBillingSectionTitle>
        <StyledBillingSectionHeader>
          <span>{t('billing_customer_invoices_title')}</span>
          {showAllButton && (
            <Button
              size="small"
              color="primary"
              variant="contained"
              onClick={() => setShowAll(true)}
            >
              {t('billing_customer_invoices_all_button')}
            </Button>
          )}
        </StyledBillingSectionHeader>
      </StyledBillingSectionTitle>
      <StyledContainer>
        <PaginatedHateoasList
          listComponent={StyledGrid}
          wrapperComponent={'div'}
          emptyPlaceholder={
            invoicesLoadable.isLoading ? (
              <BoxLoading />
            ) : (
              t('billing_invoices_empty')
            )
          }
          renderItem={(item) => (
            <>
              <StyledItem data-cy="billing-invoice-number">
                {item.number}
              </StyledItem>
              <StyledItem>{formatDate(item.createdAt)}</StyledItem>
              <StyledItem>
                {formatPrice(item.total, { maximumFractionDigits: 2 })}
              </StyledItem>
              <StyledItem>
                <Box>
                  <DownloadButton invoice={item} />
                </Box>
              </StyledItem>
            </>
          )}
          loadable={invoicesLoadable}
        />
      </StyledContainer>
      {showAll && <InvoicesModal onClose={() => setShowAll(false)} />}
    </StyledBillingSection>
  );
};
