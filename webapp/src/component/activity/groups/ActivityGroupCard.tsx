import React, { FC, PropsWithChildren, useState } from 'react';
import { Box, Chip, IconButton, styled, Tooltip } from '@mui/material';
import { ChevronDown, Code02, Terminal } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { FormatedDateTooltip } from 'tg.component/common/tooltip/FormatedDateTooltip';
import { actionsConfiguration } from 'tg.component/activity/configuration';
import { ActionType } from 'tg.component/activity/types';
import { getGroupIcon, getGroupSemantic } from './groupTypeVisuals';

type ActivityGroupModel = components['schemas']['ActivityGroupModel'];

const StyledContainer = styled('div')`
  display: grid;
  gap: 2px 12px;
  padding: 10px 14px;
  border-radius: 12px;
  background: ${({ theme }) => theme.palette.background.paper};
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  grid-template-columns: auto auto 1fr auto auto;
  grid-template-areas:
    'icon avatar content time expand'
    'expanded expanded expanded expanded expanded';
  align-items: center;
  transition: background 0.1s ease-out;

  &:hover {
    background: ${({ theme }) => theme.palette.cell.hover};
  }
`;

const StyledIconChip = styled(Box)<{ semantic: string }>`
  grid-area: icon;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  flex-shrink: 0;
  color: ${({ theme, semantic }) =>
    semantic === 'create'
      ? theme.palette.success.main
      : semantic === 'delete'
      ? theme.palette.error.main
      : theme.palette.text.secondary};
  background: ${({ theme }) => theme.palette.tokens.background.hover};

  & svg {
    width: 18px;
    height: 18px;
  }
`;

const StyledContent = styled(Box)`
  grid-area: content;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px 8px;
  min-width: 0;
`;

const StyledAuthor = styled('span')`
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const StyledLabel = styled('span')`
  min-width: 0;
`;

const StyledSourceInfo = styled('span')`
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 13px;
`;

const StyledTime = styled(Box)`
  grid-area: time;
  font-size: 12px;
  color: ${({ theme }) => theme.palette.text.secondary};
  white-space: nowrap;
`;

const StyledExpandButton = styled(IconButton)<{ expanded?: string }>`
  grid-area: expand;

  & svg {
    transition: transform 0.2s ease-in-out;
    transform: rotate(${({ expanded }) => (expanded ? '180deg' : '0deg')});
  }
`;

const StyledExpanded = styled(Box)`
  grid-area: expanded;
  overflow-x: auto;
  padding-top: 8px;
`;

const StyledOriginChip = styled(Chip)`
  height: 20px;
  font-size: 11px;

  & .MuiChip-icon {
    width: 12px;
    height: 12px;
  }
`;

type Props = PropsWithChildren<{
  item: ActivityGroupModel;
  count?: number;
  expandedContent?: React.ReactNode;
}>;

export const ActivityGroupCard: FC<Props> = (props) => {
  const [expanded, setExpanded] = useState(false);

  const author = props.item.author;
  const Icon = getGroupIcon(props.item.type);
  const semantic = getGroupSemantic(props.item.type);

  return (
    <StyledContainer
      data-cy="activity-group-item"
      data-cy-type={props.item.type}
    >
      <StyledIconChip semantic={semantic}>
        <Icon />
      </StyledIconChip>
      <Box gridArea="avatar" display="flex">
        {author && (
          <AvatarImg
            size={24}
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
        <StyledLabel>{props.children}</StyledLabel>
        {props.count !== undefined && props.count > 0 && (
          <Chip
            size="small"
            label={props.count}
            data-cy="activity-group-count"
          />
        )}
        <OriginChips item={props.item} />
        <SourceActivityInfo item={props.item} />
      </StyledContent>
      <StyledTime>
        <FormatedDateTooltip date={props.item.timestamp} />
      </StyledTime>
      {props.expandedContent !== undefined && (
        <StyledExpandButton
          size="small"
          expanded={expanded ? 'true' : undefined}
          data-cy="activity-group-expand-button"
          onClick={() => setExpanded(!expanded)}
        >
          <ChevronDown />
        </StyledExpandButton>
      )}
      {expanded && <StyledExpanded>{props.expandedContent}</StyledExpanded>}
    </StyledContainer>
  );
};

const OriginChips: FC<{ item: ActivityGroupModel }> = ({ item }) => {
  const origins = item.origins || [];
  return (
    <>
      {origins.includes('API_KEY') && (
        <Tooltip
          title={
            <T
              keyName="activity_groups_origin_api_key_hint"
              defaultValue="Done over the API with a project API key"
            />
          }
        >
          <StyledOriginChip
            size="small"
            variant="outlined"
            icon={<Code02 />}
            data-cy="activity-group-origin"
            data-cy-origin="API_KEY"
            label={
              <T
                keyName="activity_groups_origin_api_key"
                defaultValue="API key"
              />
            }
          />
        </Tooltip>
      )}
      {origins.includes('PAT') && (
        <Tooltip
          title={
            <T
              keyName="activity_groups_origin_pat_hint"
              defaultValue="Done over the API with a personal access token"
            />
          }
        >
          <StyledOriginChip
            size="small"
            variant="outlined"
            icon={<Terminal />}
            data-cy="activity-group-origin"
            data-cy-origin="PAT"
            label={
              <T keyName="activity_groups_origin_pat" defaultValue="PAT" />
            }
          />
        </Tooltip>
      )}
    </>
  );
};

/**
 * When the underlying activity is a compound action (e.g. translations set
 * while creating a key), show which action the group emerged from.
 */
const SourceActivityInfo: FC<{ item: ActivityGroupModel }> = ({ item }) => {
  const interestingSources = (item.sourceActivityTypes || []).filter(
    (type) =>
      COMPOUND_SOURCE_TYPES.includes(type) &&
      // don't attribute a group to its own natural action
      type !== (item.type as string)
  );

  if (!interestingSources.length) {
    return null;
  }

  return (
    <StyledSourceInfo data-cy="activity-group-source-info">
      <T
        keyName="activity_groups_source_info_prefix"
        defaultValue="as part of:"
      />{' '}
      {interestingSources.map((type, index) => {
        const label = actionsConfiguration[type as ActionType]?.label?.({});
        return (
          <React.Fragment key={type}>
            {index > 0 && ', '}
            {label ?? type}
          </React.Fragment>
        );
      })}
    </StyledSourceInfo>
  );
};

const COMPOUND_SOURCE_TYPES = ['COMPLEX_EDIT', 'CREATE_KEY', 'IMPORT'];
