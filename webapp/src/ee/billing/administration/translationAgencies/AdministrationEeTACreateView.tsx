import { Box, Typography } from '@mui/material';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS } from 'tg.constants/links';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { TAEditForm } from './TAEditForm';
import { useHistory } from 'react-router-dom';

export const AdministrationEeTACreateView = () => {
  const history = useHistory();

  const createPlanLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/translation-agency',
    method: 'post',
  });

  return (
    <DashboardPage>
      <BaseAdministrationView
        windowTitle="Create translation agency"
        navigation={[
          ['Translation agencies', LINKS.ADMINISTRATION_EE_TA.build()],
          ['Create agency', LINKS.ADMINISTRATION_EE_TA_CREATE.build()],
        ]}
        allCentered
        hideChildrenOnLoading={false}
      >
        <Box>
          <Typography variant="h5">Create translation agency</Typography>
          <TAEditForm
            initialData={{}}
            loading={createPlanLoadable.isLoading}
            onSubmit={(value) => {
              createPlanLoadable.mutate(
                {
                  content: {
                    'application/json': {
                      ...value,
                    },
                  },
                },
                {
                  onSuccess() {
                    history.push(LINKS.ADMINISTRATION_EE_TA.build());
                  },
                }
              );
            }}
          />
        </Box>
      </BaseAdministrationView>
    </DashboardPage>
  );
};
