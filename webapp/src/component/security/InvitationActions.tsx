import { T, useTranslate } from '@tolgee/react';
import { Box, Button, Typography } from '@mui/material';
import { useUser } from 'tg.globalContext/helpers';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { components } from 'tg.service/apiSchema.generated';
import { InvitationInfoText } from './InvitationInfoText';

type InvitationData = components['schemas']['PublicInvitationModel'];

export function InvitationActions({
  data,
  isLoading,
  onAccept,
  onDecline,
}: {
  data: InvitationData;
  isLoading: boolean;
  onAccept: () => void;
  onDecline: () => void;
}) {
  const { t } = useTranslate();
  const user = useUser();

  const inviteeEmail = data.inviteeEmail;
  const isEmailMismatch =
    !!inviteeEmail &&
    !!user &&
    user.username.toLowerCase() !== inviteeEmail.toLowerCase();

  if (isEmailMismatch) {
    return (
      <Box textAlign="center" data-cy="accept-invitation-email-mismatch">
        <Typography color="error">
          <T keyName="invitation_email_mismatch" />
        </Typography>
      </Box>
    );
  }

  return (
    <>
      <Box textAlign="center" data-cy="accept-invitation-info-text">
        <InvitationInfoText data={data} />
      </Box>
      <Box display="flex" gap={3} flexWrap="wrap" justifyContent="center">
        <LoadingButton
          loading={isLoading}
          variant="contained"
          color="primary"
          onClick={onAccept}
          data-cy="accept-invitation-accept"
        >
          {t('accept_invitation_accept')}
        </LoadingButton>
        <Button
          variant="outlined"
          onClick={onDecline}
          data-cy="accept-invitation-decline"
        >
          {t('accept_invitation_decline')}
        </Button>
      </Box>
    </>
  );
}
