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
import { useMoneyFormatter, useNumberFormatter } from 'tg.hooks/useLocale';

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

const INPUT_TOKENS_CONTEXT_AND_SCREENSHOTS = 1100;
const INPUT_TOKENS_CONTEXT_NO_SCREENSHOTS = 700;
const INPUT_TOKENS_NO_CONTEXT = 260;
const OUTPUT_TOKENS = 100;
const STRINGS_COUNT = 1000;

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

  function formatEur(value: number): string {
    return formatMoney(value, {
      currency: 'EUR',
      maximumFractionDigits: 4,
      minimumFractionDigits: 2,
    });
  }

  function creditsToEur(credits: number): string | null {
    if (pricePerMtCredit == null) return null;
    return formatEur(credits * pricePerMtCredit);
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
              <Typography variant="subtitle2" gutterBottom>
                <T
                  keyName="llm_provider_pricing_token_prices"
                  defaultValue="Token prices"
                />
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                <T
                  keyName="llm_provider_pricing_token_prices_description"
                  defaultValue="MT credits charged per token processed by this LLM provider."
                />
              </Typography>
              <Table size="small" data-cy="llm-provider-pricing-token-table">
                <TableHead>
                  <TableRow>
                    <TableCell />
                    <TableCell align="right">
                      <T
                        keyName="llm_provider_pricing_per_token_credits"
                        defaultValue="Credits per token"
                      />
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  <TableRow data-cy="llm-provider-pricing-input-row">
                    <TableCell>
                      <T
                        keyName="llm_provider_pricing_input"
                        defaultValue="Input"
                      />
                    </TableCell>
                    <TableCell align="right">
                      {formatCredits(inputPrice!)}
                    </TableCell>
                  </TableRow>
                  <TableRow data-cy="llm-provider-pricing-output-row">
                    <TableCell>
                      <T
                        keyName="llm_provider_pricing_output"
                        defaultValue="Output"
                      />
                    </TableCell>
                    <TableCell align="right">
                      {formatCredits(outputPrice!)}
                    </TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </Box>

            <Box>
              <Typography variant="subtitle2" gutterBottom>
                <T
                  keyName="llm_provider_pricing_estimate_title"
                  defaultValue="Estimated cost for 1,000 strings"
                />
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                <T
                  keyName="llm_provider_pricing_estimate_description"
                  defaultValue="Estimated costs for translating 1,000 strings. Context includes key name, description, glossary terms, and translation memory. Screenshots add visual context."
                />
              </Typography>
              <Table size="small" data-cy="llm-provider-pricing-estimate-table">
                <TableHead>
                  <TableRow>
                    <TableCell />
                    <TableCell align="right">
                      <T
                        keyName="llm_provider_pricing_tokens"
                        defaultValue="Tokens"
                      />
                    </TableCell>
                    <TableCell align="right">
                      <T
                        keyName="llm_provider_pricing_credits"
                        defaultValue="Credits"
                      />
                    </TableCell>
                    {pricePerMtCredit != null && (
                      <TableCell align="right">
                        <T
                          keyName="llm_provider_pricing_eur"
                          defaultValue="EUR"
                        />
                      </TableCell>
                    )}
                  </TableRow>
                </TableHead>
                <TableBody>
                  <TableRow data-cy="llm-provider-pricing-estimate-context-and-screenshots">
                    <TableCell>
                      <T
                        keyName="llm_provider_pricing_context_and_screenshots"
                        defaultValue="Context + screenshots"
                      />
                    </TableCell>
                    <TableCell align="right">
                      {'~'}
                      {formatCredits(
                        INPUT_TOKENS_CONTEXT_AND_SCREENSHOTS + OUTPUT_TOKENS
                      )}
                    </TableCell>
                    <TableCell align="right">
                      {'~'}
                      {formatCredits(
                        estimateCredits(
                          INPUT_TOKENS_CONTEXT_AND_SCREENSHOTS,
                          OUTPUT_TOKENS
                        )!
                      )}
                    </TableCell>
                    {pricePerMtCredit != null && (
                      <TableCell align="right">
                        {'~'}
                        {creditsToEur(
                          estimateCredits(
                            INPUT_TOKENS_CONTEXT_AND_SCREENSHOTS,
                            OUTPUT_TOKENS
                          )!
                        )}
                      </TableCell>
                    )}
                  </TableRow>
                  <TableRow data-cy="llm-provider-pricing-estimate-context-no-screenshots">
                    <TableCell>
                      <T
                        keyName="llm_provider_pricing_context_no_screenshots"
                        defaultValue="Context, no screenshots"
                      />
                    </TableCell>
                    <TableCell align="right">
                      {'~'}
                      {formatCredits(
                        INPUT_TOKENS_CONTEXT_NO_SCREENSHOTS + OUTPUT_TOKENS
                      )}
                    </TableCell>
                    <TableCell align="right">
                      {'~'}
                      {formatCredits(
                        estimateCredits(
                          INPUT_TOKENS_CONTEXT_NO_SCREENSHOTS,
                          OUTPUT_TOKENS
                        )!
                      )}
                    </TableCell>
                    {pricePerMtCredit != null && (
                      <TableCell align="right">
                        {'~'}
                        {creditsToEur(
                          estimateCredits(
                            INPUT_TOKENS_CONTEXT_NO_SCREENSHOTS,
                            OUTPUT_TOKENS
                          )!
                        )}
                      </TableCell>
                    )}
                  </TableRow>
                  <TableRow data-cy="llm-provider-pricing-estimate-no-context">
                    <TableCell>
                      <T
                        keyName="llm_provider_pricing_no_context"
                        defaultValue="No context"
                      />
                    </TableCell>
                    <TableCell align="right">
                      {'~'}
                      {formatCredits(INPUT_TOKENS_NO_CONTEXT + OUTPUT_TOKENS)}
                    </TableCell>
                    <TableCell align="right">
                      {'~'}
                      {formatCredits(
                        estimateCredits(INPUT_TOKENS_NO_CONTEXT, OUTPUT_TOKENS)!
                      )}
                    </TableCell>
                    {pricePerMtCredit != null && (
                      <TableCell align="right">
                        {'~'}
                        {creditsToEur(
                          estimateCredits(
                            INPUT_TOKENS_NO_CONTEXT,
                            OUTPUT_TOKENS
                          )!
                        )}
                      </TableCell>
                    )}
                  </TableRow>
                </TableBody>
              </Table>
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
