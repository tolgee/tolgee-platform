import { FC, useState } from 'react';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useBillingApiQuery } from '../useBillingQueryApi';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { Table, TableCell, TableRow, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { DownloadButton } from './DownloadButton';

export const Invoices: FC = () => {
  const organization = useOrganization();

  const [page, setPage] = useState(0);

  const invoicesLoadable = useBillingApiQuery({
    url: '/v2/organizations/{organizationId}/billing/invoices/',
    method: 'get',
    query: {
      page: page,
    },
    path: {
      organizationId: organization!.id,
    },
  });

  return (
    <>
      <Typography variant="h2">Invoices</Typography>
      <PaginatedHateoasList
        onPageChange={(p) => setPage(p)}
        listComponent={Table}
        renderItem={(item) => (
          <TableRow>
            <TableCell>{item.number}</TableCell>
            <TableCell>
              {new Date(item.createdAt).toLocaleDateString()}{' '}
            </TableCell>
            <TableCell>
              <T
                parameters={{ total: item.total }}
                keyName="billing_invoices_list_total"
              >
                {'{total, number, :: currency/EUR }'}
              </T>
            </TableCell>
            <TableCell>
              <DownloadButton invoice={item} />{' '}
            </TableCell>
          </TableRow>
        )}
        loadable={invoicesLoadable}
      ></PaginatedHateoasList>
    </>
  );
};
