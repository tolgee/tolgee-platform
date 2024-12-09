import { Button } from '@mui/material';
import { T } from '@tolgee/react';

export const ContactUsButton = () => {
  return (
    <Button
      size="medium"
      variant="outlined"
      color="primary"
      sx={{ alignSelf: 'center' }}
      href="mailto:info@tolgee.io"
    >
      <T keyName="billing_plan_contact_us" />
    </Button>
  );
};
