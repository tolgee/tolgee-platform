import { StatusBadge, StatusBadgeTone } from 'tg.component/common/StatusBadge';

type Props = {
  severity: string;
};

const TONES: Record<string, StatusBadgeTone> = {
  critical: 'error',
  major: 'warning',
  minor: 'teal',
};

// How serious an AI review issue is: Critical red, Major orange, Minor teal.
export const AiReviewSeverityBadge = ({ severity }: Props) => {
  const key = severity.toLowerCase();
  return (
    <StatusBadge
      tone={TONES[key] ?? 'neutral'}
      data-cy="ai-review-severity-badge"
    >
      {key.charAt(0).toUpperCase() + key.slice(1)}
    </StatusBadge>
  );
};
