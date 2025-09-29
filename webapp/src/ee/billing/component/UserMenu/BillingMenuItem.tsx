import { Box, MenuItem } from '@mui/material';
import { Link } from 'react-router-dom';
import { useTranslate } from '@tolgee/react';

import { LINKS, PARAMS } from 'tg.constants/links';
import { FC } from 'react';
import {
  useConfig,
  useOrganizationUsage,
  usePreferredOrganization,
  useUser,
} from 'tg.globalContext/helpers';
import { CircularBillingProgress } from '../CircularBillingProgress';
import { BillingMenuItemsProps } from '../../../../eeSetup/EeModuleType';
import { getProgressData } from '../getProgressData';

export const BillingMenuItem: FC<BillingMenuItemsProps> = ({ onClose }) => {
  const { t } = useTranslate();

  const { preferredOrganization } = usePreferredOrganization();

  const { usage } = useOrganizationUsage();
  const progressData = usage && getProgressData({ usage });
  const config = useConfig();
  const user = useUser()!;

  const showBilling =
    config.billing.enabled &&
    preferredOrganization &&
    (preferredOrganization?.currentUserRole === 'OWNER' ||
      user.globalServerRole === 'ADMIN' ||
      user.globalServerRole === 'SUPPORTER');

  if (!showBilling) {
    return null;
  }

  return (
    <MenuItem
      component={Link}
      to={LINKS.ORGANIZATION_SUBSCRIPTIONS.build({
        [PARAMS.ORGANIZATION_SLUG]: preferredOrganization.slug,
      })}
      onClick={onClose}
      data-cy="user-menu-organization-settings"
    >
      <Box
        display="flex"
        justifyContent="space-between"
        flexGrow="1"
        alignItems="center"
      >
        <div>{t('organization_menu_subscriptions')}</div>
        {progressData && (
          <CircularBillingProgress
            size={22}
            value={progressData.mostCriticalProgress}
            maxValue={1}
            isPayAsYouGo={usage?.isPayAsYouGo}
          />
        )}
      </Box>
    </MenuItem>
  );
};
