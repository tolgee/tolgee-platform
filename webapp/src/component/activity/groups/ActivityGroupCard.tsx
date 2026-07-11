import React, { FC, PropsWithChildren, useState } from 'react';
import { Box, Chip, IconButton, styled } from '@mui/material';
import { ChevronDown, ChevronUp } from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useDateFormatter } from 'tg.hooks/useLocale';

type ActivityGroupModel = components['schemas']['ActivityGroupModel'];

const StyledContainer = styled('div')`
  display: grid;
  gap: 8px 12px;
  padding: 12px 16px;
  border-radius: 12px;
  background: ${({ theme }) => theme.palette.background.paper};
  border: 1px solid ${({ theme }) => theme.palette.divider};
  grid-template-columns: auto 1fr auto auto;
  grid-template-areas:
    'avatar content time expand'
    'expanded expanded expanded expanded';
  align-items: center;
`;

const StyledContent = styled(Box)`
  grid-area: content;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px 8px;
  min-width: 0;
`;

const StyledAuthor = styled(Box)`
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const StyledTime = styled(Box)`
  grid-area: time;
  font-size: 12px;
  color: ${({ theme }) => theme.palette.text.secondary};
  white-space: nowrap;
`;

const StyledExpanded = styled(Box)`
  grid-area: expanded;
  overflow-x: auto;
`;

type Props = PropsWithChildren<{
  item: ActivityGroupModel;
  count?: number;
  expandedContent?: React.ReactNode;
}>;

export const ActivityGroupCard: FC<Props> = (props) => {
  const [expanded, setExpanded] = useState(false);
  const formatDate = useDateFormatter();

  const author = props.item.author;

  return (
    <StyledContainer
      data-cy="activity-group-item"
      data-cy-type={props.item.type}
    >
      <Box gridArea="avatar" display="flex">
        {author && (
          <AvatarImg
            size={32}
            owner={{
              type: 'USER',
              id: author.id,
              avatar: author.avatar,
              deleted: author.deleted,
              name: author.name,
            }}
          />
        )}
      </Box>
      <StyledContent>
        <StyledAuthor>{author?.name || author?.username}</StyledAuthor>
        <Box>{props.children}</Box>
        {props.count !== undefined && props.count > 0 && (
          <Chip
            size="small"
            label={props.count}
            data-cy="activity-group-count"
          />
        )}
      </StyledContent>
      <StyledTime>
        {formatDate(new Date(props.item.timestamp), {
          dateStyle: 'medium',
          timeStyle: 'short',
        })}
      </StyledTime>
      <Box gridArea="expand">
        {props.expandedContent !== undefined && (
          <IconButton
            size="small"
            data-cy="activity-group-expand-button"
            onClick={() => setExpanded(!expanded)}
          >
            {expanded ? <ChevronUp /> : <ChevronDown />}
          </IconButton>
        )}
      </Box>
      {expanded && <StyledExpanded>{props.expandedContent}</StyledExpanded>}
    </StyledContainer>
  );
};
