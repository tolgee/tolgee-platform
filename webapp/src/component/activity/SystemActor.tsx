import { Tooltip, useTheme } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { Stars } from 'tg.component/CustomIcons';
import { components } from 'tg.service/apiSchema.generated';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { UserName } from 'tg.component/common/UserName';

type Author = {
  id: number;
  name?: string;
  username?: string;
  avatar?: components['schemas']['Avatar'];
  deleted?: boolean;
};

type ActorProps = {
  systemActor?: string;
  author?: Author;
};

export const ActorAvatar = ({ systemActor, author }: ActorProps) => {
  if (systemActor) {
    return <SystemActorAvatar systemActor={systemActor} />;
  }
  if (!author) {
    return null;
  }
  return (
    <AvatarImg
      size={24}
      owner={{
        type: 'USER',
        id: author.id,
        name: author.name,
        avatar: author.avatar,
        deleted: author.deleted,
      }}
    />
  );
};

export const ActorName = ({
  systemActor,
  author,
  dataCySpans,
  full,
}: ActorProps & { dataCySpans?: boolean; full?: boolean }) => {
  if (systemActor) {
    return (
      <SystemActorLabel
        systemActor={systemActor}
        authorName={author?.name}
        dataCySpans={dataCySpans}
        full={full}
      />
    );
  }
  if (author?.deleted) {
    return <UserName {...author} />;
  }
  return <>{author?.name}</>;
};

type AvatarProps = {
  systemActor: string;
};

export const SystemActorAvatar = ({ systemActor }: AvatarProps) => {
  if (systemActor === 'AI_REVIEWER') {
    return <AiReviewerAvatar />;
  }
  return null;
};

type LabelProps = {
  systemActor: string;
  authorName?: string;
  dataCySpans?: boolean;
  full?: boolean;
};

export const SystemActorLabel = ({
  systemActor,
  authorName,
  dataCySpans = true,
  full = false,
}: LabelProps) => {
  if (systemActor === 'AI_REVIEWER') {
    return (
      <AiReviewerLabel
        authorName={authorName}
        dataCySpans={dataCySpans}
        full={full}
      />
    );
  }
  return null;
};

const AiReviewerAvatar = () => {
  const theme = useTheme();
  return <Stars width={24} height={24} color={theme.palette.secondary.main} />;
};

type AiReviewerLabelProps = {
  authorName?: string;
  dataCySpans: boolean;
  full: boolean;
};

const AiReviewerLabel = ({
  authorName,
  dataCySpans,
  full,
}: AiReviewerLabelProps) => {
  const { t } = useTranslate();

  if (full && authorName) {
    return (
      <T
        keyName="activity_ai_reviewer_started_by"
        defaultValue="<actor>AI reviewer</actor> · <startedby>started by {name}</startedby>"
        params={{
          actor: (
            <span data-cy="activity-system-actor" data-cy-started-by="true" />
          ),
          startedby: <span data-cy="activity-started-by" />,
          name: authorName,
        }}
      />
    );
  }

  const label = t('activity_system_actor_ai_reviewer', 'AI reviewer');
  const content = dataCySpans ? (
    <span
      data-cy="activity-system-actor"
      data-cy-started-by={String(Boolean(authorName))}
    >
      {label}
    </span>
  ) : (
    <span>{label}</span>
  );

  if (!authorName) {
    return content;
  }

  return (
    <Tooltip
      title={t(
        'activity_ai_reviewer_started_by_tooltip',
        'AI reviewer started by {name}',
        { name: authorName }
      )}
      disableInteractive
    >
      {content}
    </Tooltip>
  );
};
