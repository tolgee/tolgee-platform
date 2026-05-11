import { FilterItem } from 'tg.views/projects/translations/TranslationFilters/FilterItem';
import { useQaCheckTypeLabel } from 'tg.ee.module/qa/hooks/useQaCheckTypeLabel';
import { components } from 'tg.service/apiSchema.generated';

type QaCheckType = components['schemas']['QaIssueModel']['type'];

export function CheckTypeFilterItem({
  checkType,
  selected,
  onClick,
}: {
  checkType: QaCheckType;
  selected: boolean;
  onClick: () => void;
}) {
  const label = useQaCheckTypeLabel(checkType);
  return <FilterItem label={label} selected={selected} onClick={onClick} />;
}
