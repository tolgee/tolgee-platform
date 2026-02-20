import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

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

const INPUT_TOKENS_WITH_CONTEXT = 1100;
const INPUT_TOKENS_WITHOUT_CONTEXT = 700;
const OUTPUT_TOKENS = 100;
const STRINGS_COUNT = 1000;

function formatCredits(value: number): string {
  if (value < 0.01) {
    return value.toExponential(2);
  }
  return value.toFixed(4);
}

function formatUsd(value: number): string {
  return `$${value.toFixed(4)}`;
}

export const LlmProviderPricingDialog = ({
  provider,
  perThousandMtCredits,
  onClose,
}: Props) => {
  const { t } = useTranslate();

  const inputPrice = provider.tokenPriceInCreditsInput;
  const outputPrice = provider.tokenPriceInCreditsOutput;
  const hasPricing = inputPrice != null && outputPrice != null;
  const pricePerMtCredit =
    perThousandMtCredits != null ? perThousandMtCredits / 1000 : null;

  function creditsToUsd(credits: number): string | null {
    if (pricePerMtCredit == null) return null;
    return formatUsd(credits * pricePerMtCredit);
  }

  function estimateCredits(
    inputTokensPerString: number,
    outputTokensPerString: number
  ): number | null {
    if (inputPrice == null || outputPrice == null) return null;
    return (
      (inputTokensPerString * inputPrice +
        outputTokensPerString * outputPrice) *
      STRINGS_COUNT
    );
  }

  return (
    <Dialog open onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        {t('llm_provider_pricing_dialog_title')} — {provider.name}
      </DialogTitle>
      <DialogContent>
        {!hasPricing ? (
          <Typography color="text.secondary">
            <T keyName="llm_provider_pricing_no_pricing" />
          </Typography>
        ) : (
          <Box display="grid" gap={3}>
            <Box>
              <Typography variant="subtitle2" gutterBottom>
                <T keyName="llm_provider_pricing_token_prices" />
              </Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell />
                    <TableCell>
                      <T keyName="llm_provider_pricing_per_token_credits" />
                    </TableCell>
                    {pricePerMtCredit != null && (
                      <TableCell>
                        <T keyName="llm_provider_pricing_per_token_usd" />
                      </TableCell>
                    )}
                  </TableRow>
                </TableHead>
                <TableBody>
                  <TableRow>
                    <TableCell>
                      <T keyName="llm_provider_pricing_input" />
                    </TableCell>
                    <TableCell>{formatCredits(inputPrice!)}</TableCell>
                    {pricePerMtCredit != null && (
                      <TableCell>{creditsToUsd(inputPrice!)}</TableCell>
                    )}
                  </TableRow>
                  <TableRow>
                    <TableCell>
                      <T keyName="llm_provider_pricing_output" />
                    </TableCell>
                    <TableCell>{formatCredits(outputPrice!)}</TableCell>
                    {pricePerMtCredit != null && (
                      <TableCell>{creditsToUsd(outputPrice!)}</TableCell>
                    )}
                  </TableRow>
                </TableBody>
              </Table>
            </Box>

            <Box>
              <Typography variant="subtitle2" gutterBottom>
                <T keyName="llm_provider_pricing_estimate_title" />
              </Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell />
                    <TableCell>
                      <T keyName="llm_provider_pricing_credits" />
                    </TableCell>
                    {pricePerMtCredit != null && (
                      <TableCell>
                        <T keyName="llm_provider_pricing_per_token_usd" />
                      </TableCell>
                    )}
                  </TableRow>
                </TableHead>
                <TableBody>
                  <TableRow>
                    <TableCell>
                      <T keyName="llm_provider_pricing_with_screenshots" />
                    </TableCell>
                    <TableCell>
                      {formatCredits(
                        estimateCredits(
                          INPUT_TOKENS_WITH_CONTEXT,
                          OUTPUT_TOKENS
                        )!
                      )}
                    </TableCell>
                    {pricePerMtCredit != null && (
                      <TableCell>
                        {creditsToUsd(
                          estimateCredits(
                            INPUT_TOKENS_WITH_CONTEXT,
                            OUTPUT_TOKENS
                          )!
                        )}
                      </TableCell>
                    )}
                  </TableRow>
                  <TableRow>
                    <TableCell>
                      <T keyName="llm_provider_pricing_without_screenshots" />
                    </TableCell>
                    <TableCell>
                      {formatCredits(
                        estimateCredits(
                          INPUT_TOKENS_WITHOUT_CONTEXT,
                          OUTPUT_TOKENS
                        )!
                      )}
                    </TableCell>
                    {pricePerMtCredit != null && (
                      <TableCell>
                        {creditsToUsd(
                          estimateCredits(
                            INPUT_TOKENS_WITHOUT_CONTEXT,
                            OUTPUT_TOKENS
                          )!
                        )}
                      </TableCell>
                    )}
                  </TableRow>
                </TableBody>
              </Table>
            </Box>

            <Box display="grid" gap={1}>
              <Typography variant="body2" color="text.secondary">
                <T keyName="llm_provider_pricing_margin_note" />
              </Typography>
              <Typography variant="body2" color="text.secondary">
                <T keyName="llm_provider_pricing_plan_note" />
              </Typography>
            </Box>
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('global_close_button')}</Button>
      </DialogActions>
    </Dialog>
  );
};
