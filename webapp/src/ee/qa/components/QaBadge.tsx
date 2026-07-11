import { Badge, styled } from '@mui/material';

import { QaBadgeProps } from '../../../eeSetup/EeModuleType';
import { QaCheck } from 'tg.component/CustomIcons';
import { Check, DotsHorizontal } from '@untitled-ui/icons-react';
import clsx from 'clsx';

const StyledBadge = styled(Badge)`
  &.darker {
    opacity: 0.5;
  }

  display: flex;

  & .unresolved {
    font-size: 10px;
    height: unset;
    padding: 3px 3px;
  }
  & .resolved,
  & .stale {
    background: ${({ theme }) => theme.palette.emphasis[600]};
    padding: 0px;
    height: 16px;
    width: 18px;
    min-width: unset;
    align-items: center;
    justify-content: center;
  }
`;

const StyledCheckIcon = styled(Check)`
  color: ${({ theme }) => theme.palette.emphasis[100]};
  width: 14px !important;
  height: 14px !important;
  margin: -5px;
`;

const StyledDotsIcon = styled(DotsHorizontal)`
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
    return (
      <span data-cy="qa-badge">
        <StyledBadge
          badgeContent={<StyledDotsIcon />}
          classes={{ badge: 'stale' }}
          className={clsx({ darker: darkWhenNoIssues })}
        >
          <QaCheck />
        </StyledBadge>
      </span>
    );
  }

  if (!count) {
    return (
      <span data-cy="qa-badge">
        <StyledBadge
          badgeContent={<StyledCheckIcon />}
          classes={{ badge: 'resolved' }}
          className={clsx({ darker: darkWhenNoIssues })}
        >
          <QaCheck />
        </StyledBadge>
      </span>
    );
  }

  return (
    <span data-cy="qa-badge">
      <StyledBadge
        badgeContent={count}
        color="primary"
        classes={{ badge: 'unresolved' }}
        data-cy="qa-badge-unresolved"
      >
        <QaCheck />
      </StyledBadge>
    </span>
  );
};
