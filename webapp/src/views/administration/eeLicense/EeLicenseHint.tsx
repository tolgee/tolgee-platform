import { Alert } from '@mui/material';
import { T } from '@tolgee/react';

import { LINKS } from 'tg.constants/links';

const TOLGEE_APP = 'https://app.tolgee.io';

export function EeLicenseHint() {
  return (
    <Alert severity="info" icon={<></>}>
      <T
        keyName="ee_licence_key_hint"
        params={{
          a: (
            <a
              href={`${TOLGEE_APP}${LINKS.GO_TO_SELF_HOSTED_BILLING.build()}`}
            />
          ),
        }}
      />
    </Alert>
  );
}
