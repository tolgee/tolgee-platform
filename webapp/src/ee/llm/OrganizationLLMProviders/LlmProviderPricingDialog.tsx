import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useMoneyFormatter, useNumberFormatter } from 'tg.hooks/useLocale';
import { TokenPricesTable } from './TokenPricesTable';
import { EstimateCostTable } from './EstimateCostTable';

type ProviderWithPricing = {
  name: string;
  tokenPriceInCreditsInput?: number | null;
  tokenPriceInCreditsOutput?: number | null;
};

type Props = {
  provider: ProviderWithPricing;
  perThousandMtCredits: number | null;
  onClose: () => void;
};

export const LlmProviderPricingDialog = ({
  provider,
  perThousandMtCredits,
  onClose,
}: Props) => {
  const { t } = useTranslate();
  const formatNumber = useNumberFormatter();
  const formatMoney = useMoneyFormatter();

  const inputPrice = provider.tokenPriceInCreditsInput;
  const outputPrice = provider.tokenPriceInCreditsOutput;
  const hasPricing = inputPrice != null && outputPrice != null;
  const pricePerMtCredit =
    perThousandMtCredits != null ? perThousandMtCredits / 1000 : null;

  function formatCredits(value: number): string {
    return formatNumber(value, {
      maximumFractionDigits: 6,
      minimumFractionDigits: 0,
    });
  }

  function creditsToEur(credits: number): string | null {
    if (pricePerMtCredit == null) return null;
    return formatMoney(credits * pricePerMtCredit, {
      currency: 'EUR',
      maximumFractionDigits: 4,
      minimumFractionDigits: 2,
    });
  }

  return (
    <Dialog
      open
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      data-cy="llm-provider-pricing-dialog"
    >
      <DialogTitle>
        {t(
          'llm_provider_pricing_dialog_title',
          'Pricing information — {providerName}',
          { providerName: provider.name }
        )}
      </DialogTitle>
      <DialogContent>
        {!hasPricing ? (
          <Typography color="text.secondary">
            <T
              keyName="llm_provider_pricing_no_pricing"
              defaultValue="No pricing information available for this provider."
            />
          </Typography>
        ) : (
          <Box display="grid" gap={3}>
            <Box>
              <TokenPricesTable
                inputPrice={inputPrice!}
                outputPrice={outputPrice!}
                formatCredits={formatCredits}
              />
            </Box>
            <Box>
              <EstimateCostTable
                inputPrice={inputPrice!}
                outputPrice={outputPrice!}
                pricePerMtCredit={pricePerMtCredit}
                formatCredits={formatCredits}
                creditsToEur={creditsToEur}
              />
            </Box>
            <Typography variant="body2" color="text.secondary">
              <T
                keyName="llm_provider_pricing_plan_note"
                defaultValue="All values are estimates. Actual costs may vary depending on language pair and how much context is provided."
              />
            </Typography>
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('global_close_button')}</Button>
      </DialogActions>
    </Dialog>
  );
};
