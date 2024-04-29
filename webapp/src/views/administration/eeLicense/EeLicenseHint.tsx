import { Alert, Link } from '@mui/material';
import { T } from '@tolgee/react';

import { LINKS } from 'tg.constants/links';

const TOLGEE_APP = 'https://app.tolgee.io';

export function EeLicenseHint() {
  const link = (
    <Link href={`${TOLGEE_APP}${LINKS.GO_TO_SELF_HOSTED_BILLING.build()}`} />
  );
  return (
    <Alert severity="info">
      <T
        keyName="ee_licence_key_hint"
        params={{
          a: link,
        }}
      />
    </Alert>
  );
}
