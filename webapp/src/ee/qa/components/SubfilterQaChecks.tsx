import { useRef, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Divider, Menu } from '@mui/material';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { FiltersInternal } from 'tg.views/projects/translations/TranslationFilters/tools';
import { FilterItem } from 'tg.views/projects/translations/TranslationFilters/FilterItem';
import { SubfilterQaChecksProps } from '../../../eeSetup/EeModuleType';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { CheckTypeFilterItem } from './CheckTypeFilterItem';
import { CheckTypeFilterName } from 'tg.ee.module/qa/components/CheckTypeFilterName';

type QaCheckType = components['schemas']['QaIssueModel']['type'];

export const SubfilterQaChecks = ({
  projectId,
  value,
  actions,
}: SubfilterQaChecksProps) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);

  const { data: qaCheckCategories } = useApiQuery({
    url: '/v2/projects/{projectId}/qa-check-types',
    method: 'get',
    path: { projectId },
    options: {
      enabled: open,
      staleTime: Infinity,
    },
  });

  function handleToggleAnyQaIssues() {
    if (value.filterHasQaIssues) {
      actions.removeFilter('filterHasQaIssues');
    } else {
      actions.addFilter('filterHasQaIssues');
    }
  }

  function handleToggleCheckType(checkType: QaCheckType) {
    if (value.filterQaCheckTypes?.includes(checkType)) {
      actions.removeFilter('filterQaCheckTypes', checkType);
    } else {
      actions.addFilter('filterQaCheckTypes', checkType);
    }
  }

  return (
    <>
      <SubmenuItem
        ref={anchorEl}
        label={t('translations_filters_heading_qa_checks')}
        onClick={() => setOpen(true)}
        selected={Boolean(getQaChecksFiltersLength(value))}
        open={open}
      />
      {open && (
        <Menu
          open={open}
          anchorEl={anchorEl.current!}
          anchorOrigin={{
            vertical: 'top',
            horizontal: 'right',
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'left',
          }}
          onClose={() => {
            setOpen(false);
          }}
          slotProps={{ paper: { style: { minWidth: 250 } } }}
        >
          <FilterItem
            label={t('translation_filters_qa_has_issues')}
            selected={Boolean(value.filterHasQaIssues)}
            onClick={handleToggleAnyQaIssues}
          />
          {qaCheckCategories?.map((category) => (
            <div key={category.category}>
              <Divider />
              {category.checkTypes.map((checkType) => (
                <CheckTypeFilterItem
                  key={checkType}
                  checkType={checkType}
                  selected={
                    value.filterQaCheckTypes?.includes(checkType) ?? false
                  }
                  onClick={() => handleToggleCheckType(checkType)}
                />
              ))}
            </div>
          ))}
        </Menu>
      )}
    </>
  );
};

export function getQaChecksFiltersLength(value: FiltersInternal) {
  return (
    Number(value.filterHasQaIssues !== undefined) +
    (value.filterQaCheckTypes?.length ?? 0)
  );
}

export function getQaChecksFiltersName(value: FiltersInternal) {
  if (value.filterHasQaIssues) {
    return <T keyName="translation_filters_qa_has_issues" />;
  }
  if (value.filterQaCheckTypes?.length) {
    return <CheckTypeFilterName checkType={value.filterQaCheckTypes[0]} />;
  }
}
