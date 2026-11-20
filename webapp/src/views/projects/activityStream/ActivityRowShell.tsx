import { FunctionComponent, ReactNode, createContext, useContext } from 'react';
import { Box, ButtonBase, styled } from '@mui/material';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useTimeDistance } from 'tg.hooks/useTimeDistance';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { components } from 'tg.service/apiSchema.generated';

type Author = components['schemas']['ProjectActivityAuthorModel'];

const StyledRow = styled(ButtonBase)`
  grid-column: 1 / span 3;
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  grid-template-areas:
    'avatar title time'
    'avatar detail .';
  align-items: start;
  column-gap: 10px;
  margin: 0px 4px;
  padding: 8px 12px;
  border-radius: 8px;
  text-align: left;
  font-size: 14px;
  line-height: 1.3;
  transition: background 0.1s ease-out;

  &.static {
    cursor: default;
  }

  &:not(.static):hover {
    background: ${({ theme }) => theme.palette.cell.hover};
    transition: background 0.3s ease-in;
  }

  &.expanded {
    background: ${({ theme }) => theme.palette.cell.selected};
  }

  b {
    font-weight: 500;
  }
`;

const StyledTitle = styled(Box)`
  grid-area: title;
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 4px 6px;
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
  font-size: 12px;
  white-space: nowrap;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

export const ActivityShowPastTimesContext = createContext(false);

type Props = {
  author: Author | undefined;
  avatar?: ReactNode;
  timestamp: number;
  title: ReactNode;
  detail?: ReactNode;
  onClick?: () => void;
  chevron?: 'right' | 'down' | 'up';
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
  ...dataProps
}) => {
  const timeDistance = useTimeDistance();
  const showPastTimes = useContext(ActivityShowPastTimesContext);
  const showTime =
    showPastTimes ||
    new Date(timestamp).toDateString() === new Date().toDateString();
  const formatDate = useDateFormatter();

  return (
    <StyledRow
      onClick={onClick}
      disableRipple={!onClick}
      className={
        !onClick ? 'static' : chevron === 'up' ? 'expanded' : undefined
      }
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
      {showTime ? (
        <StyledTime
          title={formatDate(timestamp, {
            dateStyle: 'long',
            timeStyle: 'short',
          })}
        >
          {timeDistance(timestamp)}
        </StyledTime>
      ) : (
        <StyledTime />
      )}
    </StyledRow>
  );
};
