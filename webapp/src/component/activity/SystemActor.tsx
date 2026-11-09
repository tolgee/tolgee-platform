import { useTheme } from '@mui/material';
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
}: ActorProps & { dataCySpans?: boolean }) => {
  if (systemActor) {
    return (
      <SystemActorLabel
        systemActor={systemActor}
        authorName={author?.name}
        dataCySpans={dataCySpans}
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
};

export const SystemActorLabel = ({
  systemActor,
  authorName,
  dataCySpans = true,
}: LabelProps) => {
  if (systemActor === 'AI_REVIEWER') {
    return (
      <AiReviewerLabel authorName={authorName} dataCySpans={dataCySpans} />
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
};

const AiReviewerLabel = ({ authorName, dataCySpans }: AiReviewerLabelProps) => {
  const { t } = useTranslate();

  if (!authorName) {
    const label = t('activity_system_actor_ai_reviewer', 'AI reviewer');
    if (dataCySpans) {
      return <span data-cy="activity-system-actor">{label}</span>;
    }
    return <span>{label}</span>;
  }

  const params = dataCySpans
    ? {
        actor: <span data-cy="activity-system-actor" />,
        startedby: <span data-cy="activity-started-by" />,
        name: authorName,
      }
    : {
        actor: <span />,
        startedby: <span />,
        name: authorName,
      };

  return (
    <T
      keyName="activity_ai_reviewer_started_by"
      defaultValue="<actor>AI reviewer</actor> · <startedby>started by {name}</startedby>"
      params={params}
    />
  );
};
