import { Box, styled } from '@mui/material';
import clsx from 'clsx';
import { BillingProgress } from 'tg.component/billing/BillingProgress';
import { BILLING_CRITICAL_FRACTION } from 'tg.component/billing/constants';
import { useNumberFormatter } from 'tg.hooks/useLocale';

export const StyledMetrics = styled('div')`
  display: grid;
  grid-template-columns: auto auto 2fr;
  gap: 6px 8px;
  margin: 16px 0px;
  max-width: 650px;
`;

const StyledName = styled(Box)`
  grid-column: 0;
  font-size: 15px;
`;

const StyledProgress = styled(Box)`
  display: grid;
  align-items: center;
`;

const StyledValue = styled('span')`
  &.low {
    color: ${({ theme }) => theme.palette.error.main};
  }
  &.over {
    color: ${({ theme }) => theme.palette.warning.main};
  }
  &.sufficient {
    color: ${({ theme }) => theme.palette.tokens.secondary.main};
  }
`;

type Props = {
  name: string | React.ReactNode;
  currentQuantity: number;
  totalQuantity?: number;
  periodEnd?: number;
  isPayAsYouGo?: boolean;
  'data-cy'?: string;
};

export const PlanMetric: React.FC<Props> = ({
  name,
  currentQuantity,
  totalQuantity,
  isPayAsYouGo,
  ...props
}) => {
  const formatNumber = useNumberFormatter();
  const showProgress = totalQuantity !== undefined;
  const progress = currentQuantity / totalQuantity!;
  const valueClass = isPayAsYouGo
    ? totalQuantity && currentQuantity > totalQuantity
      ? 'over'
      : 'sufficient'
    : progress > BILLING_CRITICAL_FRACTION
    ? 'low'
    : 'sufficient';

  return (
    <>
      <StyledName>{name}</StyledName>
      <Box data-cy={props['data-cy']}>
        <StyledValue className={clsx({ [valueClass]: showProgress })}>
          {formatNumber(currentQuantity)}
        </StyledValue>
        <span>{showProgress ? ` / ${formatNumber(totalQuantity!)}` : ''}</span>
      </Box>
      {showProgress && (
        <StyledProgress>
          <BillingProgress
            value={currentQuantity}
            maxValue={totalQuantity}
            height={8}
            canGoOver={isPayAsYouGo}
            showLabels
          />
        </StyledProgress>
      )}
    </>
  );
};
