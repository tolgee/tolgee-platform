import { FilterItem } from 'tg.views/projects/translations/TranslationFilters/FilterItem';
import { useQaCheckTypeLabel } from 'tg.ee.module/qa/hooks/useQaCheckTypeLabel';
import { QaCheckType } from 'tg.service/apiSchemaTypes';

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
