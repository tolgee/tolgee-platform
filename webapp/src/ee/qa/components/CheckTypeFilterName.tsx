import { useQaCheckTypeLabel } from 'tg.ee.module/qa/hooks/useQaCheckTypeLabel';
import { components } from 'tg.service/apiSchema.generated';

type QaCheckType = components['schemas']['QaIssueModel']['type'];

export function CheckTypeFilterName({ checkType }: { checkType: QaCheckType }) {
  const label = useQaCheckTypeLabel(checkType as any);
  return <>{label}</>;
}
