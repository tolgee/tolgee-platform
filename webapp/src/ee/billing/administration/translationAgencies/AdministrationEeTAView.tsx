import { useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, Chip, IconButton, ListItem, ListItemText } from '@mui/material';
import { Edit02, X } from '@untitled-ui/icons-react';
import { Link } from 'react-router-dom';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS, PARAMS } from 'tg.constants/links';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { confirmation } from 'tg.hooks/confirmation';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { components } from 'tg.service/billingApiSchema.generated';

type TranslationAgencyPublicModel =
  components['schemas']['TranslationAgencyPublicModel'];

export const AdministrationEeTAView = () => {
  const messaging = useMessage();
  const { t } = useTranslate();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');

  const plansLoadable = useBillingApiQuery({
    url: '/v2/billing/translation-agency',
    method: 'get',
    query: {
      page,
      search,
      size: 20,
    },
  });

  const deleteAgencyLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/translation-agency/{agencyId}',
    method: 'delete',
    invalidatePrefix: '/v2/billing/translation-agency',
  });

  function deleteAgency(agency: TranslationAgencyPublicModel) {
    confirmation({
      hardModeText: agency.name,
      message: 'Delete translation agency?',
      onConfirm: () =>
        deleteAgencyLoadable.mutate(
          {
            path: { agencyId: agency.id },
          },
          {
            onSuccess() {
              messaging.success('Translation agency deleted');
            },
          }
        ),
    });
  }

  return (
    <DashboardPage>
      <BaseAdministrationView
        title={t('administration_ee_translation_agencies')}
        windowTitle={t('administration_ee_translation_agencies')}
        navigation={[
          [
            t('administration_ee_translation_agencies'),
            LINKS.ADMINISTRATION_EE_TA.build(),
          ],
        ]}
        allCentered
        hideChildrenOnLoading={false}
        addLinkTo={LINKS.ADMINISTRATION_EE_TA_CREATE.build()}
      >
        <PaginatedHateoasList
          onSearchChange={setSearch}
          onPageChange={setPage}
          searchText={search}
          loadable={plansLoadable}
          renderItem={(a) => (
            <ListItem
              data-cy="administration-users-list-item"
              sx={{ display: 'grid', gridTemplateColumns: '1fr auto' }}
            >
              <ListItemText>
                {a.name} <Chip size="small" label={a.id} />
              </ListItemText>
              <Box display="flex" justifyContent="center" gap={1}>
                <IconButton
                  component={Link}
                  to={LINKS.ADMINISTRATION_EE_TA_EDIT.build({
                    [PARAMS.TA_ID]: a.id,
                  })}
                >
                  <Edit02 width={20} height={20} />
                </IconButton>
                <IconButton onClick={() => deleteAgency(a)}>
                  <X width={20} height={20} />
                </IconButton>
              </Box>
            </ListItem>
          )}
        />
      </BaseAdministrationView>
    </DashboardPage>
  );
};
