import React from 'react';
import { alpha, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';

type MentionedUserModel = components['schemas']['MentionedUserModel'];

const StyledMention = styled('span')`
  color: ${({ theme }) => theme.palette.primary.main};
  background: ${({ theme }) => alpha(theme.palette.primary.main, 0.1)};
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  padding: ${({ theme }) => theme.spacing(0, 0.5)};
  font-weight: inherit;
  white-space: nowrap;
`;

const StyledUnknown = styled('span')`
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  text: string;
  mentions: MentionedUserModel[] | undefined;
};

export const MentionText = ({ text, mentions }: Props) => {
  const { t } = useTranslate();
  const parts = text.split(/(<@\d+>)/g);

  return (
    <>
      {parts.map((part, i) => {
        const match = /^<@(\d+)>$/.exec(part);
        if (!match) {
          return <React.Fragment key={i}>{part}</React.Fragment>;
        }
        const user = mentions?.find((m) => m.id === Number(match[1]));
        if (!user) {
          return (
            <StyledUnknown key={i} data-cy="comment-mention-unknown">
              {t('comment_mention_unknown_user', '@unknown user')}
            </StyledUnknown>
          );
        }
        return (
          <StyledMention
            key={i}
            data-cy="comment-mention"
            data-cy-user-id={user.id}
          >
            @{user.name}
          </StyledMention>
        );
      })}
    </>
  );
};
