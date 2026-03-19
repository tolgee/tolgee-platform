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
import { useNumberFormatter } from 'tg.hooks/useLocale';

const INPUT_TOKENS_CONTEXT_AND_SCREENSHOTS = 1100;
const INPUT_TOKENS_CONTEXT_NO_SCREENSHOTS = 700;
const INPUT_TOKENS_NO_CONTEXT = 260;
const OUTPUT_TOKENS = 100;

type Props = {
  inputPrice: number;
  outputPrice: number;
  includedMtCredits: number;
};

function creditsPerString(
  inputPrice: number,
  outputPrice: number,
  inputTokensPerString: number
): number {
  return inputTokensPerString * inputPrice + OUTPUT_TOKENS * outputPrice;
}

const CapacityRow = ({
  dataCy,
  label,
  inputTokens,
  inputPrice,
  outputPrice,
  includedMtCredits,
  formatNumber,
}: {
  dataCy: string;
  label: ReactNode;
  inputTokens: number;
  inputPrice: number;
  outputPrice: number;
  includedMtCredits: number;
  formatNumber: (value: number) => string;
}) => {
  const perString = creditsPerString(inputPrice, outputPrice, inputTokens);
  const capacity = Math.floor(includedMtCredits / perString);
  return (
    <TableRow data-cy={dataCy}>
      <TableCell>{label}</TableCell>
      <TableCell align="right">
        {'~'}
        {formatNumber(capacity)}
      </TableCell>
    </TableRow>
  );
};

export const MonthlyCreditCapacity = ({
  inputPrice,
  outputPrice,
  includedMtCredits,
}: Props) => {
  const numberFormatter = useNumberFormatter();

  function formatNumber(value: number): string {
    return numberFormatter(value, {
      maximumFractionDigits: 0,
      minimumFractionDigits: 0,
    });
  }

  const rowProps = { inputPrice, outputPrice, includedMtCredits, formatNumber };

  return (
    <>
      <Typography variant="subtitle2" gutterBottom>
        <T
          keyName="llm_provider_pricing_monthly_capacity_title"
          defaultValue="Monthly credit capacity"
        />
      </Typography>
      <Typography variant="body2" color="text.secondary" gutterBottom>
        <T
          keyName="llm_provider_pricing_monthly_capacity_description"
          defaultValue="Estimated number of strings you can translate to a single language with your monthly {credits} MT credits."
          params={{ credits: formatNumber(includedMtCredits) }}
        />
      </Typography>
      <Table
        size="small"
        data-cy="llm-provider-pricing-monthly-capacity-table"
      >
        <TableHead>
          <TableRow>
            <TableCell />
            <TableCell align="right">
              <T
                keyName="llm_provider_pricing_strings"
                defaultValue="Strings"
              />
            </TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          <CapacityRow
            dataCy="llm-provider-pricing-capacity-context-and-screenshots"
            inputTokens={INPUT_TOKENS_CONTEXT_AND_SCREENSHOTS}
            label={
              <T
                keyName="llm_provider_pricing_context_and_screenshots"
                defaultValue="Context + screenshots"
              />
            }
            {...rowProps}
          />
          <CapacityRow
            dataCy="llm-provider-pricing-capacity-context-no-screenshots"
            inputTokens={INPUT_TOKENS_CONTEXT_NO_SCREENSHOTS}
            label={
              <T
                keyName="llm_provider_pricing_context_no_screenshots"
                defaultValue="Context, no screenshots"
              />
            }
            {...rowProps}
          />
          <CapacityRow
            dataCy="llm-provider-pricing-capacity-no-context"
            inputTokens={INPUT_TOKENS_NO_CONTEXT}
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
