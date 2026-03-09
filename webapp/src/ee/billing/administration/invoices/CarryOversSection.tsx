import { FC, useState } from 'react';
import {
  Box,
  Chip,
  Link,
  ListItem,
  Paper,
  Tab,
  Tabs,
  Typography,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/billingApiSchema.generated';
import { useDateFormatter, useMoneyFormatter } from 'tg.hooks/useLocale';
import { LINKS, PARAMS } from 'tg.constants/links';

type CarryOverModel = components['schemas']['CarryOverModel'];

type AmountItemProps = {
  label: string;
  value: string;
};

const AmountItem: FC<AmountItemProps> = ({ label, value }) => (
  <Box sx={{ textAlign: 'right' }}>
    <Box sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>{label}</Box>
    <Box>{value}</Box>
  </Box>
);

type CarryOverRowProps = {
  item: CarryOverModel;
  showSettledBy?: boolean;
};

const CarryOverRow: FC<CarryOverRowProps> = ({ item, showSettledBy }) => {
  const formatPrice = useMoneyFormatter();
  const formatDate = useDateFormatter();
  const isCloud = item.subscriptionType === 'cloud';

  return (
    <ListItem>
      <Box
        display="flex"
        justifyContent="space-between"
        width="100%"
        alignItems="center"
        gap={2}
      >
        <Box display="flex" gap={2} alignItems="center" minWidth={0}>
          <Box>
            <Link
              href={LINKS.ORGANIZATION_INVOICES.build({
                [PARAMS.ORGANIZATION_SLUG]: item.organizationSlug,
              })}
            >
              {item.organizationName}
            </Link>
            <Box sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>
              {formatDate(new Date(item.periodStart).getTime())}
              {' – '}
              {formatDate(new Date(item.periodEnd).getTime())}
            </Box>
          </Box>
          <Chip
            label={isCloud ? 'Cloud' : 'Self-hosted EE'}
            size="small"
            color={isCloud ? 'primary' : 'default'}
            variant="outlined"
          />
        </Box>

        <Box display="flex" gap={3} alignItems="center" flexShrink={0}>
          {showSettledBy && item.resolvedByInvoiceNumber && (
            <Box sx={{ textAlign: 'right' }}>
              <Box sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>
                Settled by
              </Box>
              <Link
                href={LINKS.ORGANIZATION_INVOICES.build({
                  [PARAMS.ORGANIZATION_SLUG]: item.organizationSlug,
                })}
                sx={{ whiteSpace: 'nowrap' }}
              >
                {item.resolvedByInvoiceNumber}
              </Link>
            </Box>
          )}
          <AmountItem label="Credits" value={formatPrice(item.credits)} />
          <AmountItem label="Seats" value={formatPrice(item.seats)} />
          <AmountItem
            label="Translations"
            value={formatPrice(item.translations)}
          />
          <AmountItem label="Keys" value={formatPrice(item.keys)} />
          <Box sx={{ fontWeight: 600, minWidth: 64, textAlign: 'right' }}>
            {formatPrice(item.total)}
          </Box>
        </Box>
      </Box>
    </ListItem>
  );
};

type CarryOverListProps = {
  url:
    | '/v2/administration/billing/carry-overs'
    | '/v2/administration/billing/carry-overs/history';
  emptyMessage: string;
  showSettledBy?: boolean;
};

const CarryOverList: FC<CarryOverListProps> = ({
  url,
  emptyMessage,
  showSettledBy,
}) => {
  const loadable = useBillingApiQuery({
    url,
    method: 'get',
  });

  const items = loadable.data?._embedded?.carryOvers ?? [];

  if (items.length === 0 && !loadable.isLoading) {
    return (
      <Typography variant="body2" color="text.secondary">
        {emptyMessage}
      </Typography>
    );
  }

  return (
    <Paper variant="outlined">
      {items.map((item) => (
        <CarryOverRow
          key={`${item.organizationId}-${item.periodStart}`}
          item={item}
          showSettledBy={showSettledBy}
        />
      ))}
    </Paper>
  );
};

export const CarryOversSection: FC = () => {
  const { t } = useTranslate();
  const [tab, setTab] = useState<'active' | 'history'>('active');

  return (
    <Box>
      <Typography variant="h6" sx={{ mb: 2 }}>
        {t(
          'administration_invoices_carry_overs_title',
          'Deferred usage (carry-overs)'
        )}
      </Typography>

      <Tabs value={tab} onChange={(_, value) => setTab(value)} sx={{ mb: 2 }}>
        <Tab
          value="active"
          label={t('administration_carry_overs_tab_active', 'Active')}
        />
        <Tab
          value="history"
          label={t('administration_carry_overs_tab_history', 'History')}
        />
      </Tabs>

      {tab === 'active' && (
        <CarryOverList
          url="/v2/administration/billing/carry-overs"
          emptyMessage={t(
            'administration_invoices_carry_overs_empty',
            'No active carry-overs'
          )}
        />
      )}

      {tab === 'history' && (
        <CarryOverList
          url="/v2/administration/billing/carry-overs/history"
          emptyMessage={t(
            'administration_carry_overs_history_empty',
            'No resolved carry-overs'
          )}
          showSettledBy
        />
      )}
    </Box>
  );
};
