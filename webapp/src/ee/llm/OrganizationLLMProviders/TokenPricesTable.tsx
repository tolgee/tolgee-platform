import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { T } from '@tolgee/react';

type Props = {
  inputPrice: number;
  outputPrice: number;
  formatCredits: (value: number) => string;
};

export const TokenPricesTable = ({
  inputPrice,
  outputPrice,
  formatCredits,
}: Props) => {
  return (
    <>
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
              <T keyName="llm_provider_pricing_input" defaultValue="Input" />
            </TableCell>
            <TableCell align="right">{formatCredits(inputPrice)}</TableCell>
          </TableRow>
          <TableRow data-cy="llm-provider-pricing-output-row">
            <TableCell>
              <T keyName="llm_provider_pricing_output" defaultValue="Output" />
            </TableCell>
            <TableCell align="right">{formatCredits(outputPrice)}</TableCell>
          </TableRow>
        </TableBody>
      </Table>
    </>
  );
};
