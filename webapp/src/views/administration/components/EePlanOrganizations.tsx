import {
  ListItem,
  ListItemText,
  FormControlLabel,
  Checkbox,
  Box,
  Typography,
  Switch,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useState } from 'react';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { useApiQuery, useBillingApiQuery } from 'tg.service/http/useQueryApi';

type Props = {
  planId?: number;
  originalOrganizations: number[];
  organizations: number[];
  setOrganizations: (orgs: number[]) => void;
};

export function EePlanOrganizations({
  planId,
  originalOrganizations,
  organizations,
  setOrganizations,
}: Props) {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [onlyAllowed, setOnlyAllowed] = useState(false);
  const { t } = useTranslate();

  function toggleFilterOwn() {
    setPage(0);
    setSearch('');
    setOnlyAllowed(!onlyAllowed);
  }

  const planOrganizations = useBillingApiQuery({
    url: '/v2/administration/billing/self-hosted-ee-plans/{planId}/organizations',
    method: 'get',
    path: { planId: planId! },
    query: { size: 10, page, search },
    options: {
      keepPreviousData: true,
      enabled: planId !== undefined && onlyAllowed,
    },
  });

  const allOrganizations = useApiQuery({
    url: '/v2/administration/organizations',
    method: 'get',
    query: { size: 10, page, search },
    options: {
      keepPreviousData: true,
      enabled: !onlyAllowed,
    },
  });

  const newOrganizations = organizations.filter(
    (org) => !originalOrganizations.includes(org)
  );

  return (
    <Box sx={{ minHeight: 690 }}>
      <Box display="flex" justifyContent="space-between">
        <Typography sx={{ mt: 2, mb: 1 }}>
          {t('administration_cloud_plan_form_organizations_title')} (
          {organizations.length})
        </Typography>
        {planId && (
          <FormControlLabel
            control={
              <Switch checked={onlyAllowed} onChange={toggleFilterOwn} />
            }
            label={t('administration_cloud_plan_form_organizations_filter_own')}
          />
        )}
      </Box>

      {onlyAllowed && Boolean(newOrganizations.length) && (
        <Box display="flex" justifyContent="center">
          <Box fontSize={16}>
            {t('administration_cloud_plan_form_organizations_new', {
              num: newOrganizations.length,
            })}
          </Box>
        </Box>
      )}
      <PaginatedHateoasList
        listComponentProps={{ sx: { minHeight: 540 } }}
        onSearchChange={setSearch}
        searchText={search}
        onPageChange={setPage}
        loadable={!onlyAllowed ? allOrganizations : planOrganizations}
        renderItem={(o) => {
          const label = o.name !== o.slug ? `${o.name} (${o.slug})` : o.name;
          const isRemoved = !organizations.includes(o.id);
          return (
            <ListItem data-cy="administration-organizations-list-item">
              <FormControlLabel
                control={
                  <Checkbox
                    size="small"
                    checked={!isRemoved}
                    onChange={() => {
                      if (isRemoved) {
                        setOrganizations([...organizations, o.id]);
                      } else {
                        setOrganizations(
                          organizations.filter((id) => id !== o.id)
                        );
                      }
                    }}
                  />
                }
                label={<ListItemText>{label}</ListItemText>}
              />
            </ListItem>
          );
        }}
      />
    </Box>
  );
}
