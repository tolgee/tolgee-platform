import { FC, useState } from 'react';
import {
  Box,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  styled,
} from '@mui/material';

import { useOrganization } from 'tg.views/organizations/useOrganization';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { useDateFormatter, useMoneyFormatter } from 'tg.hooks/useLocale';
import { DownloadButton } from './DownloadButton';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { useTranslate } from '@tolgee/react';

const StyledGrid = styled('div')`
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  gap: 8px;
  min-width: 400px;
  min-height: 400px;
  align-content: start;
`;

const StyledItem = styled(Box)`
  display: grid;
  align-self: center;
`;

type Props = {
  onClose: () => void;
};

export const InvoicesModal: FC<Props> = ({ onClose }) => {
  const organization = useOrganization();
  const { t } = useTranslate();

  const [page, setPage] = useState(0);

  const formatPrice = useMoneyFormatter();
  const formatDate = useDateFormatter();

  const invoicesLoadable = useBillingApiQuery({
    url: '/v2/organizations/{organizationId}/billing/invoices/',
    method: 'get',
    query: {
      page: page,
      size: 10,
    },
    path: {
      organizationId: organization!.id,
    },
    options: {
      keepPreviousData: true,
    },
  });

  useGlobalLoading(invoicesLoadable.isFetching);

  return (
    <Dialog open onClose={onClose}>
      <DialogTitle>{t('billing_customer_invoices_all_title')}</DialogTitle>
      <DialogContent>
        <PaginatedHateoasList
          onPageChange={(p) => setPage(p)}
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
              <StyledItem>{item.number}</StyledItem>
              <StyledItem>{formatDate(item.createdAt)}</StyledItem>
              <StyledItem>{formatPrice(item.total)}</StyledItem>
              <StyledItem>
                <Box>
                  <DownloadButton invoice={item} />
                </Box>
              </StyledItem>
            </>
          )}
          loadable={invoicesLoadable}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={() => onClose()} color="secondary">
          {t('global_close_button')}
        </Button>
      </DialogActions>
    </Dialog>
  );
};
