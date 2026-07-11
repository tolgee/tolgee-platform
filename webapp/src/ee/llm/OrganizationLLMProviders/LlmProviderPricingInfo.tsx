import { Box, IconButton, styled, Tooltip } from '@mui/material';
import { useState } from 'react';
import { InfoCircle } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';
import {
  LlmProviderPricingDialog,
  ProviderWithPricing,
} from './LlmProviderPricingDialog';

const StyledAction = styled(Box)`
  display: flex;
  align-items: center;
  align-self: stretch;
  justify-self: stretch;
  color: ${({ theme }) => theme.palette.text.secondary};
  padding-right: 8px;
`;

type Props = {
  provider: ProviderWithPricing;
  perThousandMtCredits: number | null;
  includedMtCredits: number | null;
};

export const LlmProviderPricingInfo = ({
  provider,
  perThousandMtCredits,
  includedMtCredits,
}: Props) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);

  return (
    <>
      <StyledAction data-cy="llm-provider-pricing-info">
        <Tooltip title={t('llm_provider_pricing_info_tooltip', 'Pricing info')}>
          <IconButton
            size="small"
            color="inherit"
            onClick={() => setOpen(true)}
          >
            <InfoCircle />
          </IconButton>
        </Tooltip>
      </StyledAction>
      {open && (
        <LlmProviderPricingDialog
          provider={provider}
          perThousandMtCredits={perThousandMtCredits}
          includedMtCredits={includedMtCredits}
          onClose={() => setOpen(false)}
        />
      )}
    </>
  );
};
