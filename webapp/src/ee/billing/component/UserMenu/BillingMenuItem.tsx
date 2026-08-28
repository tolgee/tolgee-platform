import { Box, MenuItem } from '@mui/material';
import { Link } from 'react-router-dom';
import { useTranslate } from '@tolgee/react';

import { billingLinkFor } from 'tg.fixtures/billingLink';
import { FC } from 'react';
import {
  useBillingOrganization,
  useOrganizationUsage,
} from 'tg.globalContext/helpers';
import { CircularBillingProgress } from '../CircularBillingProgress';
import { BillingMenuItemsProps } from 'eeSetup/EeModuleType';
import { getProgressData } from '../getProgressData';

export const BillingMenuItem: FC<
  React.PropsWithChildren<BillingMenuItemsProps>
> = ({ onClose }) => {
  const { t } = useTranslate();

  const { usage } = useOrganizationUsage();
  const progressData = usage && getProgressData({ usage });
  const billingOrganization = useBillingOrganization();

  if (!billingOrganization) {
    return null;
  }

  return (
    <MenuItem
      component={Link}
      to={billingLinkFor({ slug: billingOrganization.slug })}
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
