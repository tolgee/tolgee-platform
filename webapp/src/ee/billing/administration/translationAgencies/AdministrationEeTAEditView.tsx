import { Box, Typography, useTheme } from '@mui/material';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS, PARAMS } from 'tg.constants/links';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { TAEditForm } from './TAEditForm';
import { useHistory, useRouteMatch } from 'react-router-dom';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { TAProfileAvatar } from './TAProfileAvatar';
import { StyledInputLabel } from 'tg.component/common/TextField';

export const AdministrationEeTAEditView = () => {
  const history = useHistory();
  const theme = useTheme();

  const match = useRouteMatch();

  const agencyId = match.params[PARAMS.TA_ID];

  const agencyLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/translation-agency/{agencyId}',
    method: 'get',
    path: { agencyId },
  });

  const editPlanLoadable = useBillingApiMutation({
    url: '/v2/administration/billing/translation-agency/{agencyId}',
    method: 'put',
  });

  const agency = agencyLoadable.data!;

  return (
    <DashboardPage>
      <BaseAdministrationView
        windowTitle="Edit translation agency"
        navigation={[
          ['Translation agencies', LINKS.ADMINISTRATION_EE_TA.build()],
          ['Edit agency', LINKS.ADMINISTRATION_EE_TA_EDIT.build()],
        ]}
        allCentered
        hideChildrenOnLoading={false}
      >
        {agencyLoadable.isLoading ? (
          <BoxLoading />
        ) : (
          <Box>
            <Typography variant="h5">Edit translation agency</Typography>
            <TAEditForm
              initialData={{
                name: agency.name,
                email: agency.email,
                emailBcc: agency.emailBcc,
                description: agency.description,
                services: agency.services,
                url: agency.url,
              }}
              loading={editPlanLoadable.isLoading}
              onSubmit={(value) => {
                editPlanLoadable.mutate(
                  {
                    path: { agencyId },
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
              avatarEdit={
                <Box display="grid" gap={0.5}>
                  <StyledInputLabel>Avatar (600x30px)</StyledInputLabel>
                  <Box
                    sx={{
                      background: theme.palette.tokens.background.floating,
                    }}
                  >
                    <TAProfileAvatar agency={agency} />
                  </Box>
                </Box>
              }
            />
          </Box>
        )}
      </BaseAdministrationView>
    </DashboardPage>
  );
};
