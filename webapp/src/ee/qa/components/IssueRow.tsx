import { Box, styled, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useQaCheckTypeLabel } from 'tg.ee.module/qa/hooks/useQaCheckTypeLabel';
import { components } from 'tg.service/apiSchema.generated';

type QaCheckType = components['schemas']['QaIssueModel']['type'];

const StyledRow = styled(Box)`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;

  &:hover {
    background: ${({ theme }) => theme.palette.tokens.text._states.hover};
  }

  & .show-link {
    visibility: hidden;
  }

  &:hover .show-link {
    visibility: visible;
  }
`;

const StyledShowLink = styled(Typography)`
  font-size: 13px;
  color: ${({ theme }) => theme.palette.primary.main};
  cursor: pointer;
`;

type IssueRowProps = {
  checkType: QaCheckType;
  count: number;
  onClick: () => void;
};

export function IssueRow({ checkType, count, onClick }: IssueRowProps) {
  const { t } = useTranslate();
  const label = useQaCheckTypeLabel(checkType);

  return (
    <StyledRow onClick={onClick}>
      <Typography variant="body2">
        {label}: {count}
      </Typography>
      <StyledShowLink className="show-link" variant="button">
        {t('qa_dashboard_popover_show')}
      </StyledShowLink>
    </StyledRow>
  );
}
