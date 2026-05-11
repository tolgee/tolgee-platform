import { useQaCheckTypeTooltip } from 'tg.ee.module/qa/hooks/useQaCheckTypeTooltip';
import { getQaCheckTypeDocLink } from 'tg.ee.module/qa/hooks/getQaCheckTypeDocLink';
import { styled, Tooltip } from '@mui/material';
import { InfoCircle } from '@untitled-ui/icons-react';
import { components } from 'tg.service/apiSchema.generated';
import { LinkReadMore } from 'tg.component/LinkReadMore';

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
  const docLink = getQaCheckTypeDocLink(type);
  if (!tooltip) {
    return null;
  }

  const title = docLink ? (
    <>
      {tooltip} <LinkReadMore url={docLink} />
    </>
  ) : (
    tooltip
  );

  return (
    <Tooltip title={title}>
      <span style={{ display: 'inline-flex' }}>
        <StyledInfoIcon />
      </span>
    </Tooltip>
  );
};
