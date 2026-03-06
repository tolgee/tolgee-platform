import { IconButton, styled, Tooltip } from '@mui/material';
import { ChevronUp } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';
import clsx from 'clsx';

const StyledCounterContainer = styled('div')`
  display: flex;
  background: ${({ theme }) => theme.palette.background.paper};
  align-items: stretch;
  transition: opacity 0.3s ease-in-out;
  border-radius: 6px;
  -webkit-box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
  box-shadow: 2px 2px 5px rgba(0, 0, 0, 0.25);
  margin: ${({ theme }) => theme.spacing(2, 3, 2, 0)};
  white-space: nowrap;
  pointer-events: all;

  &.hidden {
    opacity: 0;
    pointer-events: none;
  }
`;

const StyledDivider = styled('div')`
  border-right: 1px solid ${({ theme }) => theme.palette.divider};
`;

const StyledIndex = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  margin-right: ${({ theme }) => theme.spacing(2)};
  margin-left: ${({ theme }) => theme.spacing(1)};
`;

const StyledStretcher = styled('div')`
  font-family: monospace;
  height: 0px;
  overflow: hidden;
`;

const StyledIconButton = styled(IconButton)`
  flex-shrink: 0;
  width: 40px;
  height: 40px;
`;

type Props = {
  scrollIndex: number;
  totalCount: number;
  visible: boolean;
  onScrollUp: () => void;
  dataCyPrefix?: string;
};

export const FloatingScrollCounter: React.FC<Props> = ({
  scrollIndex,
  totalCount,
  visible,
  onScrollUp,
  dataCyPrefix = 'translations',
}) => {
  const { t } = useTranslate();
  const counterContent = `${scrollIndex} / ${totalCount}`;

  return (
    <StyledCounterContainer className={clsx({ hidden: !visible })}>
      <StyledIndex>
        <span data-cy={`${dataCyPrefix}-toolbar-counter`}>
          {counterContent}
        </span>
        <StyledStretcher>{counterContent}</StyledStretcher>
      </StyledIndex>
      <StyledDivider />
      <Tooltip title={t('translations_toolbar_to_top')} disableInteractive>
        <StyledIconButton
          data-cy={`${dataCyPrefix}-toolbar-to-top`}
          onClick={onScrollUp}
          size="small"
          aria-label={t('translations_toolbar_to_top')}
        >
          <ChevronUp width={20} height={20} />
        </StyledIconButton>
      </Tooltip>
    </StyledCounterContainer>
  );
};
