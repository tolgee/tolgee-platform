import { FunctionComponent } from 'react';
import { Box, styled } from '@mui/material';
import { T } from '@tolgee/react';

import {
  NotificationItem,
  NotificationItemProps,
} from 'tg.component/layout/Notifications/NotificationItem';
import { getKeyCommentsUrl } from 'tg.constants/links';
import { MentionText } from 'tg.component/mentions/MentionText';

const StyledMeta = styled(Box)`
  font-size: ${({ theme }) => theme.typography.caption.fontSize};
  color: ${({ theme }) => theme.palette.text.secondary};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const StyledPreview = styled(Box)`
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  white-space: pre-wrap;
  margin-top: ${({ theme }) => theme.spacing(0.25)};
`;

export const CommentMentionItem: FunctionComponent<NotificationItemProps> = ({
  notification,
  ...props
}) => {
  const comment = notification.linkedComment;
  const destinationUrl =
    comment && notification.project
      ? getKeyCommentsUrl(
          notification.project.id,
          comment.keyId,
          comment.languageTag
        )
      : undefined;

  return (
    <NotificationItem
      notification={notification}
      destinationUrl={destinationUrl}
      {...props}
    >
      <Box data-cy="notifications-comment-mention-item">
        <Box>
          <T
            keyName="notifications-comment-mention"
            defaultValue="<b>{name}</b> mentioned you in a comment"
            params={{
              name: notification.originatingUser?.name ?? '',
              b: <b />,
            }}
          />
        </Box>
        {comment && (
          <>
            <StyledMeta data-cy="notifications-comment-mention-key">
              <T
                keyName="notifications-comment-mention-key"
                defaultValue="{keyName}, {languageTag}"
                params={{
                  keyName: comment.keyNamespace
                    ? `${comment.keyNamespace}:${comment.keyName}`
                    : comment.keyName,
                  languageTag: comment.languageTag,
                }}
              />
            </StyledMeta>
            <StyledPreview data-cy="notifications-comment-mention-preview">
              <MentionText text={comment.text} mentions={comment.mentions} />
            </StyledPreview>
          </>
        )}
      </Box>
    </NotificationItem>
  );
};
