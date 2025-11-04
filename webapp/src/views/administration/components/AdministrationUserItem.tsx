import { useDateFormatter } from 'tg.hooks/useLocale';
import { useTranslate } from '@tolgee/react';
import { Box, Chip, ListItem, Typography, useTheme } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { DebugCustomerAccountButton } from './DebugCustomerAccountButton';
import { RoleSelector } from './RoleSelector';
import { OptionsButton } from './OptionsButton';
import { MfaBadge } from 'tg.component/MfaBadge';

type UserAccountModel = components['schemas']['UserAccountAdministrationModel'];

type Props = {
  user: UserAccountModel;
  onRoleSelect: () => void;
};

export const AdministrationUserItem: React.FC<Props> = ({
  user,
  onRoleSelect,
}) => {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();
  const theme = useTheme();

  return (
    <ListItem
      data-cy="administration-users-list-item"
      sx={{ display: 'grid', gridTemplateColumns: '1fr auto' }}
    >
      <Box>
        <Box>
          <Typography variant="body1">
            {user.name} | {user.username} <Chip size="small" label={user.id} />
          </Typography>
        </Box>
        <Box>
          <Typography
            variant="body2"
            color={theme.palette.text.secondary}
            data-cy="administration-user-activity"
          >
            {!user.lastActivity
              ? t('administration_user_no_activity')
              : t('administration_user_last_activity', {
                  date: formatDate(new Date(user.lastActivity), {
                    dateStyle: 'long',
                    timeStyle: 'short',
                  }),
                })}
          </Typography>
        </Box>
      </Box>
      <Box display="flex" justifyContent="center" gap={1}>
        <MfaBadge enabled={user.mfaEnabled} />
        <DebugCustomerAccountButton userId={user.id} />
        <RoleSelector user={user} onSuccess={onRoleSelect} />
        <OptionsButton user={user} />
      </Box>
    </ListItem>
  );
};
