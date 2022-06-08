import { Box, styled } from '@mui/material';
import { BillingProgress } from 'tg.component/billing/BillingProgress';
import { useNumberFormatter } from 'tg.hooks/useLocale';

export const StyledMetrics = styled('div')`
  display: grid;
  grid-template-columns: min(30%, 250px) auto 2fr auto;
  gap: 4px 8px;
  margin: 16px 0px;
`;

const StyledName = styled(Box)`
  grid-column: 0;
  font-size: 15px;
`;

const StyledProgress = styled(Box)`
  padding-top: 9px;
`;

type Props = {
  name: string;
  currentAmount: number;
  totalAmount?: number;
  periodEnd?: number;
};

export const PlanMetric: React.FC<Props> = ({
  name,
  currentAmount,
  totalAmount,
}) => {
  const formatNumber = useNumberFormatter();

  const showProgress = totalAmount !== undefined;

  return (
    <>
      <StyledName>{name}</StyledName>
      <Box>{formatNumber(currentAmount)}</Box>
      {showProgress && (
        <>
          <StyledProgress>
            <BillingProgress percent={(currentAmount / totalAmount!) * 100} />
          </StyledProgress>
          <Box>{formatNumber(totalAmount!)}</Box>
        </>
      )}
    </>
  );
};
