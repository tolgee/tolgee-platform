import { FunctionComponent } from 'react';
import { LINKS } from 'tg.constants/links';
import { BaseUserSettingsView } from 'tg.views/userSettings/BaseUserSettingsView';
import { useTranslate } from '@tolgee/react';
import { Box, styled, Switch, Tooltip, Typography } from '@mui/material';

const StyledRoot = styled(Box)`
  display: grid;
  grid-template-columns: 1fr 120px 120px;
  grid-auto-rows: auto;
  grid-row-gap: 10px;
`;

const StyledTableHeader = styled(Box)`
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 13px;
  text-align: center;
`;

const StyledSwitch = styled(Box)`
  text-align: center;
`;

export const NotificationsView: FunctionComponent = () => {
  const { t } = useTranslate();
  return (
    <BaseUserSettingsView
      windowTitle={t('settings_notifications_title')}
      title={t('settings_notifications_title')}
      loading={false} // TODO
      navigation={[
        [
          t('user_menu_notifications'),
          LINKS.USER_ACCOUNT_NOTIFICATIONS.build(),
        ],
      ]}
      hideChildrenOnLoading={false} // TODO
    >
      <StyledRoot>
        <Box></Box>
        <StyledTableHeader>In-App</StyledTableHeader>
        <StyledTableHeader>Email</StyledTableHeader>
        <Box>
          <Box>
            <Typography variant="body1">Account security</Typography>
          </Box>
          <Box>
            <Typography
              variant="body2"
              color={(theme) => theme.palette.text.secondary}
            >
              Password-changed, Two-Factor authentication on/off
            </Typography>
          </Box>
        </Box>
        <Box>
          <Tooltip title={'Cannot be turned off'}>
            <StyledSwitch>
              <Switch defaultChecked disabled={true} />
            </StyledSwitch>
          </Tooltip>
        </Box>
        <Box>
          <Tooltip title={'Cannot be turned off'}>
            <StyledSwitch>
              <Switch defaultChecked disabled={true} />
            </StyledSwitch>
          </Tooltip>
        </Box>
        <Box>
          <Box>
            <Typography variant="body1">Tasks</Typography>
          </Box>
          <Box>
            <Typography
              variant="body2"
              color={(theme) => theme.palette.text.secondary}
            >
              Assigned, completed, closed
            </Typography>
          </Box>
        </Box>
        <StyledSwitch>
          <Switch defaultChecked />
        </StyledSwitch>
        <StyledSwitch>
          <Switch defaultChecked />
        </StyledSwitch>
      </StyledRoot>
    </BaseUserSettingsView>
  );
};
