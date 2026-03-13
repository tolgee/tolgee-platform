import { Badge, CircularProgress, styled } from '@mui/material';

import { QaBadgeProps } from '../../../eeSetup/EeModuleType';
import { QaCheck } from 'tg.component/CustomIcons';
import { Check } from '@untitled-ui/icons-react';

const StyledBadge = styled(Badge)`
  display: flex;

  & .unresolved {
    font-size: 10px;
    height: unset;
    padding: 3px 3px;
  }
  & .resolved {
    background: ${({ theme }) => theme.palette.emphasis[600]};
    padding: 0px;
    height: 16px;
    width: 18px;
    min-width: unset;
    align-items: center;
    justify-content: center;
  }
  & .stale {
    background: ${({ theme }) => theme.palette.emphasis[600]};
    padding: 0px;
    height: 16px;
    width: 18px;
    min-width: unset;
    align-items: center;
    justify-content: center;
    overflow: hidden;
  }
`;

const StyledSpinner = styled(CircularProgress)`
  color: ${({ theme }) => theme.palette.emphasis[100]};
  width: 10px !important;
  height: 10px !important;
  margin: -5px;

  & svg {
    width: 10px;
    height: 10px;
  }
`;

const StyledCheckIcon = styled(Check)`
  color: ${({ theme }) => theme.palette.emphasis[100]};
  width: 14px !important;
  height: 14px !important;
  margin: -5px;
`;

export const QaBadge = ({
  count,
  stale,
  darkWhenNoIssues = false,
}: QaBadgeProps) => {
  if (stale) {
    const hasIssues = count !== undefined && count > 0;
    return (
      <StyledBadge
        badgeContent={<StyledSpinner size={10} thickness={5} />}
        classes={{ badge: 'stale' }}
      >
        <QaCheck
          style={hasIssues || !darkWhenNoIssues ? undefined : { opacity: 0.5 }}
        />
      </StyledBadge>
    );
  }

  if (!count || count === 0) {
    return (
      <StyledBadge
        badgeContent={<StyledCheckIcon />}
        classes={{ badge: 'resolved' }}
      >
        <QaCheck style={!darkWhenNoIssues ? undefined : { opacity: 0.5 }} />
      </StyledBadge>
    );
  }

  return (
    <StyledBadge
      badgeContent={count}
      color="primary"
      classes={{ badge: 'unresolved' }}
    >
      <QaCheck />
    </StyledBadge>
  );
};
