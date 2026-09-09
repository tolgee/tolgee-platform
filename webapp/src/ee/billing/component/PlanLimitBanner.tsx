import { Link as MuiLink } from '@mui/material';
import { T } from '@tolgee/react';
import { AlertTriangle } from '@untitled-ui/icons-react';
import { Link as RouterLink } from 'react-router-dom';

import { Announcement } from 'tg.component/layout/TopBanner/Announcement';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { getProgressData } from './getProgressData';

export function usePlanLimitBanner() {
  const { preferredOrganization } = usePreferredOrganization();
  const usage = useGlobalContext((c) => c.organizationUsage?.usage);

  if (!usage || !getProgressData({ usage }).isExceeded) {
    return null;
  }

  const isOwner = preferredOrganization?.currentUserRole === 'OWNER';

  return (
    <Announcement
      icon={<AlertTriangle />}
      content={
        <span data-cy="plan-limit-banner">
          {isOwner ? (
            <T
              keyName="plan_limit_banner_reached"
              defaultValue="You've reached the limits of your plan"
            />
          ) : (
            <T
              keyName="plan_limit_banner_reached_member"
              defaultValue="Your organization has reached the limits of its plan. Contact an organization owner to upgrade."
            />
          )}
        </span>
      }
      action={
        isOwner && (
          <MuiLink
            component={RouterLink}
            color="inherit"
            underline="always"
            to={LINKS.ORGANIZATION_BILLING.build({
              [PARAMS.ORGANIZATION_SLUG]: preferredOrganization.slug,
            })}
            data-cy="plan-limit-banner-upgrade-link"
          >
            <T keyName="plan_limit_dialog_go_to_billing" />
          </MuiLink>
        )
      }
    />
  );
}
