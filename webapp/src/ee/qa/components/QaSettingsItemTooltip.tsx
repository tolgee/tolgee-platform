import { useQaCheckTypeTooltip } from 'tg.ee.module/qa/hooks/useQaCheckTypeTooltip';
import { styled, Tooltip } from '@mui/material';
import { InfoCircle } from '@untitled-ui/icons-react';
import { components } from 'tg.service/apiSchema.generated';

type QaCheckType = components['schemas']['QaIssueModel']['type'];

const StyledInfoIcon = styled(InfoCircle)`
  width: 16px;
  height: 16px;
  color: ${({ theme }) => theme.palette.text.secondary};
  cursor: help;
`;

type Props = {
  type: QaCheckType;
};

export const QaSettingsItemTooltip = ({ type }: Props) => {
  const tooltip = useQaCheckTypeTooltip(type);
  if (!tooltip) {
    return null;
  }

  return (
    <Tooltip title={tooltip}>
      <span style={{ display: 'inline-flex' }}>
        <StyledInfoIcon />
      </span>
    </Tooltip>
  );
};
