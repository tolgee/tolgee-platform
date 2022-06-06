import { FC, useState } from 'react';
import { styled, Table, TableCell, TableRow } from '@mui/material';

import { useOrganization } from 'tg.views/organizations/useOrganization';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { useDateFormatter, useMoneyFormatter } from 'tg.hooks/useLocale';
import { DownloadButton } from './DownloadButton';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { useTranslate } from '@tolgee/react';

const StyledTableRow = styled(TableRow)`
  td {
    padding: 0px;
  }

  td:first-of-type {
    padding-left: 15px;
  }
`;

const CustomTable: React.FC<React.ComponentProps<typeof Table>> = ({
  children,
}) => (
  <Table>
    <tbody>{children}</tbody>
  </Table>
);

export const Invoices: FC = () => {
  const organization = useOrganization();
  const t = useTranslate();

  const [page, setPage] = useState(0);

  const formatPrice = useMoneyFormatter();
  const formatDate = useDateFormatter();

  const invoicesLoadable = useBillingApiQuery({
    url: '/v2/organizations/{organizationId}/billing/invoices/',
    method: 'get',
    query: {
      page: page,
      size: 5,
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
    <>
      <PaginatedHateoasList
        onPageChange={(p) => setPage(p)}
        listComponent={CustomTable}
        listComponentProps={{ size: 'small' }}
        emptyPlaceholder={
          invoicesLoadable.isLoading ? (
            <BoxLoading />
          ) : (
            t('billing_invoices_empty')
          )
        }
        renderItem={(item) => (
          <StyledTableRow>
            <TableCell>{item.number}</TableCell>
            <TableCell>{formatDate(item.createdAt)}</TableCell>
            <TableCell>{formatPrice(item.total)}</TableCell>
            <TableCell>
              <DownloadButton invoice={item} />
            </TableCell>
          </StyledTableRow>
        )}
        loadable={invoicesLoadable}
      ></PaginatedHateoasList>
    </>
  );
};
