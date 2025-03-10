import { Box, styled } from '@mui/material';
import clsx from 'clsx';
import { useNumberFormatter } from 'tg.hooks/useLocale';
import { BillingProgress } from '../component/BillingProgress';
import { BILLING_CRITICAL_FRACTION } from '../component/constants';
import { ProgressItem } from '../component/getProgressData';

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
  progress: ProgressItem;
  periodEnd?: number;
  isPayAsYouGo?: boolean;
  'data-cy'?: string;
};

export const PlanMetric: React.FC<Props> = ({
  name,
  progress,
  isPayAsYouGo,
  ...props
}) => {
  const formatNumber = useNumberFormatter();
  const showProgress = progress.included !== undefined;
  const valueClass = isPayAsYouGo
    ? progress?.included && progress.used > progress.included
      ? 'over'
      : 'sufficient'
    : progress?.progress ?? 0 > BILLING_CRITICAL_FRACTION
    ? 'low'
    : 'sufficient';

  return (
    <>
      <StyledName>{name}</StyledName>
      <Box data-cy={props['data-cy']}>
        <StyledValue className={clsx({ [valueClass]: showProgress })}>
          {formatNumber(progress?.used ?? 0)}
        </StyledValue>
        <span>
          {showProgress ? ` / ${formatNumber(progress.included)}` : ''}
        </span>
      </Box>
      {showProgress && (
        <StyledProgress>
          <BillingProgress
            progressItem={progress}
            height={8}
            isPayAsYouGo={isPayAsYouGo}
            showLabels
          />
        </StyledProgress>
      )}
    </>
  );
};
