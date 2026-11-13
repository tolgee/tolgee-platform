import { Link as MuiLink } from '@mui/material';
import { T } from '@tolgee/react';
import { AlertTriangle } from '@untitled-ui/icons-react';

import { Announcement } from 'tg.component/layout/TopBanner/Announcement';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';

export function usePastDueBanner() {
  const { preferredOrganization } = usePreferredOrganization();

  const customerPortalMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/customer-portal',
    method: 'get',
    options: {
      onSuccess: (data) => {
        window.location.href = data.url;
      },
    },
  });

  if (preferredOrganization?.activeCloudSubscription?.status !== 'PAST_DUE') {
    return null;
  }

  const isOwner = preferredOrganization.currentUserRole === 'OWNER';

  return (
    <Announcement
      icon={<AlertTriangle />}
      content={
        <span data-cy="past-due-banner">
          {isOwner ? (
            <T
              keyName="past_due_banner"
              defaultValue="The payment for your subscription failed."
            />
          ) : (
            <T
              keyName="past_due_banner_member"
              defaultValue="The payment for your organization's subscription failed. Contact an organization owner."
            />
          )}
        </span>
      }
      action={
        isOwner && (
          <MuiLink
            component="button"
            onClick={() =>
              customerPortalMutation.mutate({
                path: { organizationId: preferredOrganization.id },
              })
            }
            sx={(theme) => ({
              color: theme.palette.tokens._components.noticeBar.importantLink,
              textDecoration: 'underline',
              font: 'inherit',
              '&:hover': {
                color:
                  theme.palette.tokens._components.noticeBar.importantLinkHover,
              },
            })}
            data-cy="past-due-banner-update-payment-link"
          >
            <T
              keyName="billing_past_due_update_payment"
              defaultValue="Update payment method"
            />
          </MuiLink>
        )
      }
    />
  );
}
