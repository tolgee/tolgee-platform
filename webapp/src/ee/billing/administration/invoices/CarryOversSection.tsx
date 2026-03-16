import { FC, useState } from 'react';
import { Box, Tab, Tabs, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { CarryOverList } from './CarryOverList';

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
