import { T } from '@tolgee/react';
import { useConfig, usePreferredOrganization } from 'tg.globalContext/helpers';
import { Link as MuiLink, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { MtHint } from './billing/MtHint';
import { useErrorTranslation } from 'tg.translationTools/useErrorTranslation';

type Props = {
  code: string;
};

export const NoCreditsHint = ({ code }: Props) => {
  const { preferredOrganization } = usePreferredOrganization();
  const translateError = useErrorTranslation();
  const config = useConfig();
  const canGoToBilling =
    preferredOrganization?.currentUserRole === 'OWNER' &&
    config.billing.enabled;

  if (code === 'out_of_credits') {
    if (canGoToBilling) {
      return (
        <T
          keyName="translation_tools_no_credits_billing_link"
          params={{
            link: (
              <MuiLink
                component={Link}
                to={LINKS.ORGANIZATION_BILLING.build({
                  [PARAMS.ORGANIZATION_SLUG]: preferredOrganization.slug,
                })}
              />
            ),
            hint: <MtHint />,
          }}
        />
      );
    } else {
      return (
        <T
          keyName="translation_tools_no_credits_message"
          params={{
            hint: <MtHint />,
          }}
        />
      );
    }
  } else if (code === 'credit_spending_limit_exceeded') {
    return (
      <T
        keyName="translation_tools_limit_exceeded_message"
        params={{
          hint: <MtHint />,
          email: (
            <MuiLink
              target="_blank"
              href="mailto:billing@tolgee.io"
              rel="noreferrer noopener"
            />
          ),
        }}
      />
    );
  }
  return <Typography color="red">{translateError(code)}</Typography>;
};
