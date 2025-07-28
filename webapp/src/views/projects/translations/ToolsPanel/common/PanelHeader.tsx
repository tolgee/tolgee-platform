import { Box, styled, SxProps } from '@mui/material';
import { ChevronDown, ChevronUp } from '@untitled-ui/icons-react';

const StyledHeader = styled(Box)`
  max-width: 100%;
  position: sticky;
  display: grid;
  grid-template-columns: auto auto auto 1fr;
  top: -1px;
  padding: 8px;
  padding-left: 16px;
  gap: 8px;
  align-items: center;
  background: ${({ theme }) => theme.palette.background.default};
  z-index: 3;
  height: 39px;
`;

const StyledName = styled(Box)`
  flex-shrink: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 15px;
  text-transform: uppercase;
  font-weight: 500;
`;

const StyledBadge = styled(Box)`
  padding: 2px 4px;
  border-radius: 12px;
  font-size: 12px;
  height: 20px;
  min-width: 20px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  background: ${({ theme }) => theme.palette.emphasis[100]};
`;

const StyledToggle = styled(Box)`
  display: grid;
  cursor: pointer;
`;

type Props = {
  icon: React.ReactNode;
  hideCount?: boolean;
  countContent: React.ReactNode;
  onToggle: () => void;
  panelId: string;
  open: boolean;
  name: React.ReactNode;
  sx?: SxProps;
};

export const PanelHeader = ({
  icon,
  hideCount,
  countContent,
  panelId,
  onToggle,
  open,
  name,
  sx,
}: Props) => {
  return (
    <StyledHeader onMouseDown={(e) => e.preventDefault()} sx={sx}>
      {icon}
      <StyledName>{name}</StyledName>
      {!hideCount && countContent !== undefined ? (
        <StyledBadge data-cy="translation-panel-items-count">
          {countContent}
        </StyledBadge>
      ) : (
        <div />
      )}
      <StyledToggle
        role="button"
        onClick={() => onToggle()}
        data-cy="translation-panel-toggle"
        data-cy-id={panelId}
      >
        {open ? (
          <ChevronUp width={20} height={20} />
        ) : (
          <ChevronDown width={20} height={20} />
        )}
      </StyledToggle>
    </StyledHeader>
  );
};
