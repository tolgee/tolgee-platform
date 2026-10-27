import { useState } from 'react';
import { Box, Link as MuiLink, styled, Tooltip } from '@mui/material';
import { Link } from 'react-router-dom';
import { MessageTextSquare02, Translate01 } from '@untitled-ui/icons-react';

import { LINKS, PARAMS } from 'tg.constants/links';
import { actionsConfiguration } from 'tg.component/activity/configuration';
import { StyledReferences } from 'tg.component/activity/references/AnyReference';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { useTimeDistance } from 'tg.hooks/useTimeDistance';
import { components } from 'tg.service/apiSchema.generated';

type CommunityActivityModel = components['schemas']['CommunityActivityModel'];
type ActivityType = CommunityActivityModel['type'];

const COMMENT_TYPES: ActivityType[] = [
  'TRANSLATION_COMMENT_ADD',
  'TRANSLATION_COMMENT_EDIT',
  'TRANSLATION_COMMENT_SET_STATE',
  'TRANSLATION_COMMENT_DELETE',
];

const StyledContainer = styled('div')`
  display: contents;

  & > * {
    padding: 4px 8px;
    overflow: hidden;
    transition: background 0.1s ease-out;
  }

  &:hover > * {
    transition: background 0.3s ease-in;
    background: ${({ theme }) => theme.palette.cell.hover};
  }
`;

const StyledIconCell = styled('div')`
  grid-column: 1;
  border-radius: 8px 0px 0px 8px;
  margin-left: 4px;
`;

const StyledIcon = styled('div')`
  margin-top: 2px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: ${({ theme }) => theme.palette.emphasis[50]};
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledMeta = styled('div')`
  grid-column: 2;
  grid-row: 1 / span 2;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  max-width: 160px;
  min-width: 0;
  padding-top: 2px;
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.secondary};
  white-space: nowrap;
`;

const StyledContent = styled('div')`
  grid-column: 2 / span 2;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  column-gap: 16px;
  row-gap: 2px;
  align-content: start;

  & > :not(.activityMeta) {
    grid-column: 1;
  }

  min-width: 0;
  border-radius: 0px 8px 8px 0px;
  margin-right: 4px;
`;

const StyledTitle = styled('div')`
  display: flex;
  gap: 5px;
  overflow: hidden;
`;

const StyledValue = styled('div')`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

type Relation = {
  entityId: number;
  data: Record<string, unknown>;
  relations?: Record<string, Relation>;
};

type Entity = {
  entityId: number;
  modifications?: Record<string, { old?: unknown; new?: unknown }>;
  relations?: Record<string, Relation>;
};

const firstEntity = (a: CommunityActivityModel): Entity | undefined => {
  const entities =
    a.modifiedEntities?.TranslationSuggestion ??
    a.modifiedEntities?.TranslationComment ??
    a.modifiedEntities?.Translation ??
    [];
  return entities[0] as Entity | undefined;
};

const asText = (value: unknown) =>
  typeof value === 'string' ? value : undefined;

type Props = {
  activity: CommunityActivityModel;
};

export const CommunityActivityItem = ({ activity }: Props) => {
  const [textTruncated, setTextTruncated] = useState(false);
  const formatDate = useDateFormatter();
  const timeDistance = useTimeDistance();
  const isComment = COMMENT_TYPES.includes(activity.type);
  const entity = firstEntity(activity);

  const key =
    entity?.relations?.key ?? entity?.relations?.translation?.relations?.key;
  const language = entity?.relations?.language;
  const keyName = asText(key?.data.name);

  const text = isComment
    ? asText(entity?.modifications?.text?.new) ??
      asText(entity?.modifications?.text?.old)
    : asText(entity?.modifications?.translation?.new) ??
      asText(entity?.modifications?.text?.new) ??
      (actionsConfiguration[activity.type]?.description?.(activity as any) as
        | string
        | undefined);

  return (
    <StyledContainer data-cy="community-profile-activity-item">
      <StyledIconCell>
        <StyledIcon>
          {isComment ? (
            <MessageTextSquare02 width={16} height={16} />
          ) : (
            <Translate01 width={16} height={16} />
          )}
        </StyledIcon>
      </StyledIconCell>
      <StyledContent>
        <StyledTitle>
          <Box whiteSpace="nowrap">
            {actionsConfiguration[activity.type]?.label?.({ references: 1 })}
          </Box>
          {keyName && (
            <StyledReferences>
              <span className="reference referenceComposed">
                <span className="referenceText">{keyName}</span>
                {language && (
                  <CircledLanguageIcon
                    size={14}
                    flag={asText(language.data.flagEmoji)}
                  />
                )}
              </span>
            </StyledReferences>
          )}
        </StyledTitle>
        {text && (
          <Tooltip
            title={textTruncated ? text : ''}
            disableInteractive
            placement="bottom-start"
          >
            <StyledValue
              onMouseEnter={(e) =>
                setTextTruncated(
                  e.currentTarget.scrollWidth > e.currentTarget.clientWidth
                )
              }
              data-cy="community-profile-activity-text"
            >
              {text}
            </StyledValue>
          </Tooltip>
        )}
        <StyledMeta className="activityMeta">
          <Tooltip
            title={formatDate(new Date(activity.timestamp), {
              dateStyle: 'long',
              timeStyle: 'short',
            })}
          >
            <span>{timeDistance(activity.timestamp)}</span>
          </Tooltip>
          <MuiLink
            component={Link}
            to={LINKS.PROJECT_DASHBOARD.build({
              [PARAMS.PROJECT_ID]: activity.project.id,
            })}
            color="inherit"
            underline="hover"
            noWrap
            sx={{ '&:hover': { color: 'primary.main' } }}
          >
            {activity.project.name}
          </MuiLink>
        </StyledMeta>
      </StyledContent>
    </StyledContainer>
  );
};
