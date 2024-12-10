import { Box, MenuItem } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { AgencyLabel } from 'tg.ee';
import { Select } from 'tg.component/common/Select';

type Props = {
  value: number;
  onChange: (value: number) => void;
  error?: boolean;
  helperText?: string;
};

export const AgencySelect = ({ value, onChange, error, helperText }: Props) => {
  const { t } = useTranslate();
  const agenciesLoadable = useBillingApiQuery({
    url: '/v2/billing/translation-agency',
    method: 'get',
    query: {
      size: 1000,
    },
  });

  const data = agenciesLoadable.data?._embedded?.translationAgencies;

  return (
    <Select
      sx={{ width: 200 }}
      label={t('project_members_dialog_agency')}
      minHeight={false}
      value={value ?? []}
      placeholder="test"
      onChange={(e) => onChange(e.target.value as number)}
      error={helperText}
      data-cy="agency-select"
    >
      {data?.map((agency) => (
        <MenuItem
          key={agency.id}
          value={agency.id}
          data-cy="agency-select-item"
        >
          <Box display="flex" alignItems="center" sx={{ minHeight: 25 }}>
            <AgencyLabel agency={agency} />
          </Box>
        </MenuItem>
      ))}
    </Select>
  );
};
