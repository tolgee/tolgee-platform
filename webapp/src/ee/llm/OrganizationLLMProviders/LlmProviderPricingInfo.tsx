import { Box, IconButton, styled, Tooltip } from '@mui/material';
import { useState } from 'react';
import { InfoCircle } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';
import { LlmProviderPricingDialog } from './LlmProviderPricingDialog';

const StyledAction = styled(Box)`
  display: flex;
  align-items: center;
  align-self: stretch;
  justify-self: stretch;
  color: ${({ theme }) => theme.palette.text.secondary};
  padding-right: 8px;
`;

type ProviderWithPricing = {
  name: string;
  tokenPriceInCreditsInput?: number;
  tokenPriceInCreditsOutput?: number;
};

type Props = {
  provider: ProviderWithPricing;
  perThousandMtCredits: number | null;
};

export const LlmProviderPricingInfo = ({
  provider,
  perThousandMtCredits,
}: Props) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);

  return (
    <>
      <StyledAction data-cy="llm-provider-pricing-info">
        <Tooltip title={t('llm_provider_pricing_dialog_title')}>
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
          onClose={() => setOpen(false)}
        />
      )}
    </>
  );
};
