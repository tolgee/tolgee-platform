import { FC, useState } from 'react';
import { useDebounce } from 'use-debounce';
import {
  Autocomplete,
  Box,
  Chip,
  CircularProgress,
  Link,
  ListItem,
  Paper,
  TextField,
  Typography,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/billingApiSchema.generated';
import { useDateFormatter, useMoneyFormatter } from 'tg.hooks/useLocale';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { AdminDownloadButton } from './AdminDownloadButton';
import { AdminInvoiceUsage } from './AdminInvoiceUsage';
import { LINKS, PARAMS } from 'tg.constants/links';

type InvoiceModel = components['schemas']['InvoiceModel'];

type OrgItem = {
  id: number;
  name: string;
  deleted: boolean;
};

export const OrgInvoicesSection: FC = () => {
  const { t } = useTranslate();
  const [search, setSearch] = useState('');
  const [searchDebounced] = useDebounce(search, 500);
  const [selectedOrg, setSelectedOrg] = useState<OrgItem | null>(null);
  const [invoicePage, setInvoicePage] = useState(0);

  const orgsLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/organizations',
    method: 'get',
    query: {
      search: searchDebounced,
      page: 0,
      size: 20,
    },
    options: {
      keepPreviousData: true,
    },
  });

  const orgItems: OrgItem[] =
    orgsLoadable.data?._embedded?.organizations?.map((o) => ({
      id: o.organization.id,
      name: o.organization.name,
      deleted: o.deletedAt != null,
    })) ?? [];

  const invoicesLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/invoices',
    method: 'get',
    query: {
      organizationId: selectedOrg?.id,
      page: invoicePage,
      size: 10,
    },
    options: {
      keepPreviousData: true,
    },
  });

  const formatPrice = useMoneyFormatter();
  const formatDate = useDateFormatter();

  return (
    <Box data-cy="admin-invoices-org-section">
      <Typography variant="h6" sx={{ mb: 2 }}>
        {t(
          'administration_invoices_org_invoices_title',
          'Organization invoices'
        )}
      </Typography>
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 2 }}>
        <Autocomplete<OrgItem>
          data-cy="admin-invoices-org-filter"
          options={orgItems}
          getOptionLabel={(o) => o.name}
          renderOption={(props, o) => (
            <Box
              component="li"
              {...props}
              data-cy="admin-invoices-org-filter-option"
              sx={{ display: 'flex', gap: 1, alignItems: 'center' }}
            >
              <span>{o.name}</span>
              {o.deleted && (
                <Chip
                  data-cy="admin-invoices-org-filter-option-deleted-chip"
                  label={t(
                    'administration_invoices_org_filter_deleted_chip',
                    'deleted'
                  )}
                  size="small"
                  color="default"
                  variant="outlined"
                />
              )}
            </Box>
          )}
          isOptionEqualToValue={(a, b) => a.id === b.id}
          loading={orgsLoadable.isFetching}
          value={selectedOrg}
          onChange={(_, value) => {
            setSelectedOrg(value);
            setInvoicePage(0);
          }}
          onInputChange={(_, value) => setSearch(value)}
          sx={{ width: 280 }}
          renderInput={(params) => (
            <TextField
              {...params}
              label={t(
                'administration_invoices_filter_org',
                'Filter by organization'
              )}
              size="small"
              InputProps={{
                ...params.InputProps,
                endAdornment: (
                  <>
                    {orgsLoadable.isFetching ? (
                      <CircularProgress size={16} />
                    ) : null}
                    {params.InputProps.endAdornment}
                  </>
                ),
              }}
            />
          )}
        />
      </Box>
      <PaginatedHateoasList
        onPageChange={(p) => setInvoicePage(p)}
        listComponent={Paper}
        listComponentProps={{
          variant: 'outlined',
          sx: {
            display: 'grid',
            gridTemplateColumns: 'auto auto 1fr auto auto auto',
            alignItems: 'center',
          },
        }}
        wrapperComponent={'div'}
        emptyPlaceholder={
          <Typography variant="body2" color="text.secondary">
            {t('billing_invoices_empty')}
          </Typography>
        }
        renderItem={(item: InvoiceModel) => (
          <ListItem data-cy="admin-invoices-item" sx={{ display: 'contents' }}>
            <Box data-cy="admin-invoices-item-number" sx={{ px: 2, py: 1.5 }}>
              {item.number}
            </Box>
            <Box
              sx={{
                px: 2,
                py: 1.5,
                fontSize: '0.875rem',
                color: 'text.secondary',
              }}
            >
              {formatDate(item.createdAt)}
            </Box>
            <Box sx={{ px: 2, py: 1.5 }}>
              {!selectedOrg && item.organizationSlug && (
                <Link
                  href={LINKS.ORGANIZATION_INVOICES.build({
                    [PARAMS.ORGANIZATION_SLUG]: item.organizationSlug,
                  })}
                >
                  {item.organizationName}
                </Link>
              )}
            </Box>
            <Box />
            <Box
              data-cy="admin-invoices-item-total"
              sx={{ pl: 1, pr: 0, py: 1.5, fontWeight: 500 }}
            >
              {formatPrice(item.total)}
            </Box>
            <Box
              sx={{
                pl: 0.5,
                pr: 2,
                py: 1.5,
                display: 'flex',
                gap: 0,
                alignItems: 'center',
              }}
            >
              <AdminDownloadButton invoice={item} />
              <AdminInvoiceUsage invoice={item} />
            </Box>
          </ListItem>
        )}
        loadable={invoicesLoadable}
      />
    </Box>
  );
};
