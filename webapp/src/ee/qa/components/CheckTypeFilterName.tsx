import { useQaCheckTypeLabel } from 'tg.ee.module/qa/hooks/useQaCheckTypeLabel';
import { QaCheckType } from 'tg.service/apiSchemaTypes';

export function CheckTypeFilterName({ checkType }: { checkType: QaCheckType }) {
  const label = useQaCheckTypeLabel(checkType);
  return <>{label}</>;
}
