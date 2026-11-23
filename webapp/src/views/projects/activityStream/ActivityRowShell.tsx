import { FunctionComponent, ReactNode, createContext } from 'react';
import { Box, ButtonBase, styled, Tooltip } from '@mui/material';
import { ChevronDown, ChevronRight, ChevronUp } from '@untitled-ui/icons-react';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { components } from 'tg.service/apiSchema.generated';

type Author = components['schemas']['ProjectActivityAuthorModel'];

const StyledRow = styled(ButtonBase)`
  grid-column: 1 / span 3;
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto auto;
  grid-template-areas:
    'avatar title time chevron'
    'avatar detail . .';
  align-items: start;
  column-gap: 10px;
  margin: 0px 4px;
  padding: 8px 6px 8px 10px;
  border-radius: 8px;
  text-align: left;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.3;
  transition: background 0.1s ease-out;

  &.single {
    align-items: center;
  }

  &.static {
    cursor: default;
  }

  &:not(.static):hover {
    background: ${({ theme }) => theme.palette.cell.hover};
    transition: background 0.3s ease-in;
  }

  & .hover-only {
    opacity: 0;
    transition: opacity 0.15s;
  }

  &:hover .hover-only,
  &:focus-visible .hover-only {
    opacity: 1;
  }

  &.expanded {
    background: ${({ theme }) => theme.palette.cell.selected};
  }

  b {
    font-weight: 600;
  }
`;

const StyledTitle = styled(Box)`
  grid-area: title;
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 2px 4px;
  min-width: 0;

  & > [data-cy='activity-group-flags'],
  & > [data-cy='activity-service-icon'] {
    align-self: center;
  }
`;

const StyledDetail = styled(Box)`
  grid-area: detail;
  min-width: 0;
  margin-top: 4px;
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledTime = styled(Box)`
  grid-area: time;
  align-self: center;
  font-size: 12px;
  white-space: nowrap;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const chevronSx = {
  gridArea: 'chevron',
  alignSelf: 'center',
  ml: 1,
  width: 18,
  height: 18,
  color: 'text.secondary',
} as const;

export const ActivityShowPastTimesContext = createContext(false);

type Props = {
  author: Author | undefined;
  avatar?: ReactNode;
  timestamp: number;
  title: ReactNode;
  detail?: ReactNode;
  onClick?: () => void;
  chevron?: 'right' | 'down' | 'up';
  singleLine?: boolean;
  'data-cy': string;
  'data-cy-type'?: string;
};

export const ActivityRowShell: FunctionComponent<Props> = ({
  author,
  avatar,
  timestamp,
  title,
  detail,
  onClick,
  chevron = 'right',
  singleLine,
  ...dataProps
}) => {
  const formatDate = useDateFormatter();

  return (
    <StyledRow
      onClick={onClick}
      disableRipple={!onClick}
      className={[
        !onClick ? 'static' : chevron === 'up' ? 'expanded' : '',
        singleLine ? 'single' : '',
      ].join(' ')}
      {...dataProps}
    >
      <Box gridArea="avatar" display="flex">
        {avatar}
        {!avatar && author && (
          <AvatarImg
            size={32}
            owner={{
              type: 'USER',
              id: author.id,
              avatar: author.avatar,
              deleted: author.deleted,
            }}
          />
        )}
      </Box>
      <StyledTitle>{title}</StyledTitle>
      {detail && <StyledDetail>{detail}</StyledDetail>}
      <Tooltip
        title={formatDate(timestamp, {
          dateStyle: 'medium',
          timeStyle: 'short',
        })}
        placement="top"
        disableInteractive
      >
        <StyledTime>{formatDate(timestamp, { timeStyle: 'short' })}</StyledTime>
      </Tooltip>
      {onClick && (
        <Box
          component={
            chevron === 'up'
              ? ChevronUp
              : chevron === 'down'
              ? ChevronDown
              : ChevronRight
          }
          className={chevron === 'right' ? 'hover-only' : undefined}
          sx={chevronSx}
        />
      )}
    </StyledRow>
  );
};
