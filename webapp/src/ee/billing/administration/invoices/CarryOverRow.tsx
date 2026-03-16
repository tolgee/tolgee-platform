import { FC } from 'react';
import { Box, Chip, Link, ListItem } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/billingApiSchema.generated';
import { useDateFormatter, useMoneyFormatter } from 'tg.hooks/useLocale';
import { LINKS, PARAMS } from 'tg.constants/links';
import { AmountItem } from './AmountItem';

type CarryOverModel = components['schemas']['CarryOverModel'];

type CarryOverRowProps = {
  item: CarryOverModel;
  showSettledBy?: boolean;
};

export const CarryOverRow: FC<CarryOverRowProps> = ({
  item,
  showSettledBy,
}) => {
  const formatPrice = useMoneyFormatter();
  const formatDate = useDateFormatter();
  const { t } = useTranslate();
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
            label={
              isCloud
                ? t('admin_organization_subscriptions_cloud')
                : t('admin_organization_subscriptions_self_hosted')
            }
            size="small"
            color={isCloud ? 'primary' : 'default'}
            variant="outlined"
          />
        </Box>

        <Box display="flex" gap={3} alignItems="center" flexShrink={0}>
          {showSettledBy && item.resolvedByInvoiceNumber && (
            <Box sx={{ textAlign: 'right' }}>
              <Box sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>
                {t('administration_carry_overs_settled_by')}
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
          <AmountItem
            label={t('administration_carry_overs_credits', 'Credits')}
            value={formatPrice(item.credits)}
          />
          <AmountItem
            label={t('administration_carry_overs_seats', 'Seats')}
            value={formatPrice(item.seats)}
          />
          <AmountItem
            label={t('administration_carry_overs_translations', 'Translations')}
            value={formatPrice(item.translations)}
          />
          <AmountItem
            label={t('administration_carry_overs_keys', 'Keys')}
            value={formatPrice(item.keys)}
          />
          <Box sx={{ fontWeight: 600, minWidth: 64, textAlign: 'right' }}>
            {formatPrice(item.total)}
          </Box>
        </Box>
      </Box>
    </ListItem>
  );
};
