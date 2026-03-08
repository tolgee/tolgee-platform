import { useRef, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Menu } from '@mui/material';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { FiltersInternal } from 'tg.views/projects/translations/TranslationFilters/tools';
import { FilterItem } from 'tg.views/projects/translations/TranslationFilters/FilterItem';
import { SubfilterQaChecksProps } from '../../../eeSetup/EeModuleType';

export const SubfilterQaChecks = ({
  value,
  actions,
}: SubfilterQaChecksProps) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);

  function handleSetValue(newValue: boolean) {
    if (newValue) {
      actions.addFilter('filterHasQaIssues');
    } else {
      actions.removeFilter('filterHasQaIssues');
    }
  }

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
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
            onClick={() => handleSetValue(!value.filterHasQaIssues)}
          />
        </Menu>
      )}
    </>
  );
};

export function getQaChecksFiltersLength(value: FiltersInternal) {
  return Number(value.filterHasQaIssues !== undefined);
}

export function getQaChecksFiltersName(value: FiltersInternal) {
  if (value.filterHasQaIssues) {
    return <T keyName="translation_filters_qa_has_issues" />;
  }
}
