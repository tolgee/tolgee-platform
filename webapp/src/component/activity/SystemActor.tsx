import { useTheme } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { Stars } from 'tg.component/CustomIcons';

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
    if (dataCySpans) {
      return (
        <span data-cy="activity-system-actor">
          {t('activity_system_actor_ai_reviewer', 'AI reviewer')}
        </span>
      );
    }
    return <span>{t('activity_system_actor_ai_reviewer', 'AI reviewer')}</span>;
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
