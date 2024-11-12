import { FunctionComponent, useEffect } from 'react';
import { useHistory, useLocation } from 'react-router-dom';
import { T, useTranslate } from '@tolgee/react';

import { BaseOrganizationSettingsView } from 'tg.views/organizations/components/BaseOrganizationSettingsView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { PlansCloud } from './cloud/PlansCloud';
import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { Box, ButtonGroup } from '@mui/material';
import { PlansSelfHosted } from './selfHosted/PlansSelfHosted';
import { ButtonGroupRouterItem } from 'tg.component/common/ButtonGroupRouter';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';

export const OrganizationSubscriptionsView: FunctionComponent = () => {
  const { search, pathname } = useLocation();
  const params = new URLSearchParams(search);
  const history = useHistory();
  const { refetchInitialData } = useGlobalActions();

  const success = params.has('success');
  const mtCreditsSuccess = params.has('buy-mt-credits-success');

  const messaging = useMessage();

  const organization = useOrganization();

  const { t } = useTranslate();

  const url = new URL(window.location.href);

  url.search = '';

  const refreshSubscription = useBillingApiMutation({
    url: `/v2/organizations/{organizationId}/billing/refresh-subscription`,
    method: `put`,
    invalidatePrefix: `/`,
  });

  useEffect(() => {
    if (success) {
      refreshSubscription.mutate(
        {
          path: { organizationId: organization!.id },
        },
        {
          onSuccess() {
            refetchInitialData();
          },
        }
      );
      messaging.success(<T keyName="billing_plan_update_success_message" />);
      history.replace(pathname);
    }
  }, [success]);

  useEffect(() => {
    if (mtCreditsSuccess) {
      messaging.success(
        <T keyName="billing_mt_credit_purchase_success_message" />
      );
      history.replace(pathname);
    }
  }, [mtCreditsSuccess]);

  return (
    <BaseOrganizationSettingsView
      hideChildrenOnLoading={true}
      loading={refreshSubscription.isLoading}
      link={LINKS.ORGANIZATION_BILLING}
      navigation={[
        [
          t('organization_menu_subscriptions'),
          LINKS.ORGANIZATION_BILLING.build({ slug: organization!.slug }),
        ],
      ]}
      windowTitle={t({ key: 'organization_subscriptions_title', noWrap: true })}
      maxWidth="wide"
    >
      <Box
        mb={2}
        mt={2}
        display="flex"
        justifyContent="center"
        alignItems="start"
      >
        <ButtonGroup>
          <ButtonGroupRouterItem
            data-cy="billing-subscriptions-cloud-button"
            link={LINKS.ORGANIZATION_SUBSCRIPTIONS.build({
              [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
            })}
            exact={true}
          >
            {t('organization_subscriptions_cloud_button')}
          </ButtonGroupRouterItem>
          <ButtonGroupRouterItem
            data-cy="billing-subscriptions-self-hosted-ee-button"
            exact={true}
            link={LINKS.ORGANIZATION_SUBSCRIPTIONS_SELF_HOSTED_EE.build({
              [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
            })}
          >
            {t('organization_subscriptions_self_hosted_ee_button')}
          </ButtonGroupRouterItem>
        </ButtonGroup>
      </Box>

      <PrivateRoute exact path={LINKS.ORGANIZATION_SUBSCRIPTIONS.template}>
        <PlansCloud />
      </PrivateRoute>
      <PrivateRoute
        exact
        path={LINKS.ORGANIZATION_SUBSCRIPTIONS_SELF_HOSTED_EE.template}
      >
        <PlansSelfHosted />
      </PrivateRoute>
    </BaseOrganizationSettingsView>
  );
};
