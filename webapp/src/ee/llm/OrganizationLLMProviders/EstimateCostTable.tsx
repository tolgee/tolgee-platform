import { ReactNode } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { T } from '@tolgee/react';

const INPUT_TOKENS_CONTEXT_AND_SCREENSHOTS = 1100;
const INPUT_TOKENS_CONTEXT_NO_SCREENSHOTS = 700;
const INPUT_TOKENS_NO_CONTEXT = 260;
const OUTPUT_TOKENS = 100;
const STRINGS_COUNT = 1000;

type Props = {
  inputPrice: number;
  outputPrice: number;
  pricePerMtCredit: number | null;
  formatCredits: (value: number) => string;
  creditsToEur: (credits: number) => string | null;
};

function estimateCredits(
  inputPrice: number,
  outputPrice: number,
  inputTokensPerString: number
): number {
  return (
    (inputTokensPerString * inputPrice + OUTPUT_TOKENS * outputPrice) *
    STRINGS_COUNT
  );
}

const EstimateRow = ({
  dataCy,
  label,
  inputTokens,
  inputPrice,
  outputPrice,
  pricePerMtCredit,
  formatCredits,
  creditsToEur,
}: {
  dataCy: string;
  label: ReactNode;
  inputTokens: number;
  inputPrice: number;
  outputPrice: number;
  pricePerMtCredit: number | null;
  formatCredits: (value: number) => string;
  creditsToEur: (credits: number) => string | null;
}) => {
  const totalTokens = inputTokens + OUTPUT_TOKENS;
  const credits = estimateCredits(inputPrice, outputPrice, inputTokens);
  return (
    <TableRow data-cy={dataCy}>
      <TableCell>{label}</TableCell>
      <TableCell align="right">
        {'~'}
        {formatCredits(totalTokens)}
      </TableCell>
      <TableCell align="right">
        {'~'}
        {formatCredits(credits)}
      </TableCell>
      {pricePerMtCredit != null && (
        <TableCell align="right">
          {'~'}
          {creditsToEur(credits)}
        </TableCell>
      )}
    </TableRow>
  );
};

export const EstimateCostTable = (props: Props) => {
  const { pricePerMtCredit, ...rowProps } = props;
  return (
    <>
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
                defaultValue="Tokens (per string)"
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
                <T keyName="llm_provider_pricing_eur" defaultValue="EUR" />
              </TableCell>
            )}
          </TableRow>
        </TableHead>
        <TableBody>
          <EstimateRow
            dataCy="llm-provider-pricing-estimate-context-and-screenshots"
            inputTokens={INPUT_TOKENS_CONTEXT_AND_SCREENSHOTS}
            pricePerMtCredit={pricePerMtCredit}
            label={
              <T
                keyName="llm_provider_pricing_context_and_screenshots"
                defaultValue="Context + screenshots"
              />
            }
            {...rowProps}
          />
          <EstimateRow
            dataCy="llm-provider-pricing-estimate-context-no-screenshots"
            inputTokens={INPUT_TOKENS_CONTEXT_NO_SCREENSHOTS}
            pricePerMtCredit={pricePerMtCredit}
            label={
              <T
                keyName="llm_provider_pricing_context_no_screenshots"
                defaultValue="Context, no screenshots"
              />
            }
            {...rowProps}
          />
          <EstimateRow
            dataCy="llm-provider-pricing-estimate-no-context"
            inputTokens={INPUT_TOKENS_NO_CONTEXT}
            pricePerMtCredit={pricePerMtCredit}
            label={
              <T
                keyName="llm_provider_pricing_no_context"
                defaultValue="No context"
              />
            }
            {...rowProps}
          />
        </TableBody>
      </Table>
    </>
  );
};
