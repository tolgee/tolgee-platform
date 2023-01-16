import { MenuItem, Box } from '@mui/material';
import { Link } from 'react-router-dom';
import { useTranslate } from '@tolgee/react';

import { LINKS, PARAMS } from 'tg.constants/links';
import { CircularBillingProgress } from 'tg.component/billing/CircularBillingProgress';
import { ProgressData } from 'tg.component/billing/utils';

type Props = {
  onClose: () => void;
  progressData?: ProgressData;
  organizationSlug: string;
};

export const BillingItem: React.FC<Props> = ({
  progressData,
  onClose,
  organizationSlug,
}) => {
  const { t } = useTranslate();

  return (
    <MenuItem
      component={Link}
      to={LINKS.ORGANIZATION_BILLING.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
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
        <div>{t('organization_menu_billing')}</div>
        {progressData && (
          <CircularBillingProgress
            size={22}
            percent={progressData.smallerProgress}
          />
        )}
      </Box>
    </MenuItem>
  );
};
