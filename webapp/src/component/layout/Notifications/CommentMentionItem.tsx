import { FunctionComponent } from 'react';
import { Box, styled, Tooltip } from '@mui/material';
import { T } from '@tolgee/react';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';

import {
  NotificationItem,
  NotificationItemProps,
} from 'tg.component/layout/Notifications/NotificationItem';
import { getKeyCommentsUrl } from 'tg.constants/links';
import { MentionText } from 'tg.component/mentions/MentionText';
import { useUser } from 'tg.globalContext/helpers';
import { NotificationUserName } from 'tg.component/layout/Notifications/NotificationUserName';

const StyledKeyLine = styled(Box)`
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
`;

const StyledKey = styled('span')`
  color: ${({ theme }) => theme.palette.primary.main};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const StyledPreview = styled(Box)`
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.secondary};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
`;

const withoutOwnMention = (text: string, userId: number | undefined) =>
  userId === undefined
    ? text
    : text.replace(new RegExp(`^\\s*<@${userId}>[\\s,:]*`), '');

export const CommentMentionItem: FunctionComponent<NotificationItemProps> = ({
  notification,
  ...props
}) => {
  const comment = notification.linkedComment;
  const destinationUrl =
    comment && notification.project
      ? getKeyCommentsUrl(
          notification.project.id,
          notification.id,
          comment.languageTag
        )
      : undefined;
  const flag = comment && languageInfo[comment.languageTag]?.flags?.[0];
  const user = useUser();

  return (
    <NotificationItem
      notification={notification}
      destinationUrl={destinationUrl}
      {...props}
    >
      <Box data-cy="notifications-comment-mention-item" minWidth={0}>
        <Box>
          <T
            keyName="notifications-comment-mention"
            defaultValue="<b>{name}</b> <nowrap>mentioned you in a comment</nowrap>"
            params={{
              name: notification.originatingUser?.name ?? '',
              b: (
                <NotificationUserName
                  name={notification.originatingUser?.name ?? ''}
                />
              ),
              nowrap: <span style={{ whiteSpace: 'nowrap' }} />,
            }}
          />
        </Box>
        {comment && (
          <>
            <StyledKeyLine data-cy="notifications-comment-mention-key">
              <StyledKey>
                {comment.keyNamespace
                  ? `${comment.keyNamespace}:${comment.keyName}`
                  : comment.keyName}
              </StyledKey>
              {flag && (
                <FlagImage
                  flagEmoji={flag}
                  height={16}
                  style={{ flexShrink: 0 }}
                />
              )}
            </StyledKeyLine>
            <Tooltip
              title={
                <Box whiteSpace="pre-wrap">
                  <MentionText
                    text={comment.text}
                    mentions={comment.mentions}
                  />
                </Box>
              }
              placement="bottom-start"
              enterDelay={300}
            >
              <StyledPreview data-cy="notifications-comment-mention-preview">
                <MentionText
                  text={withoutOwnMention(comment.text, user?.id)}
                  mentions={comment.mentions}
                />
              </StyledPreview>
            </Tooltip>
          </>
        )}
      </Box>
    </NotificationItem>
  );
};
