import { useRef, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { SubmenuItem } from 'tg.component/SubmenuItem';
import { AssigneeSearchSelectPopover } from 'tg.ee.module/task/components/assigneeSelect/AssigneeSearchSelectPopover';
import { FiltersInternal, FilterActions } from './tools';

type Props = {
  projectId: number;
  value: FiltersInternal;
  actions: FilterActions;
};

export const SubfilterDeletedBy = ({ value, actions, projectId }: Props) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);

  const selectedIds = value.filterDeletedByUserId ?? [];

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('trash_filter_deleted_by')}
        onClick={() => setOpen(true)}
        selected={Boolean(selectedIds.length)}
        open={open}
      />
      {open && (
        <AssigneeSearchSelectPopover
          open={open}
          onClose={() => setOpen(false)}
          anchorEl={anchorEl.current!}
          selected={selectedIds.map((id) => ({
            id,
            name: '',
            username: '',
          }))}
          onSelectImmediate={(users) =>
            actions.setFilters({
              ...value,
              filterDeletedByUserId: users.length
                ? users.map((u) => u.id)
                : undefined,
            })
          }
          projectId={projectId}
          anchorOrigin={{
            vertical: 'top',
            horizontal: 'right',
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'left',
          }}
        />
      )}
    </>
  );
};

export function getDeletedByFiltersLength(value: FiltersInternal) {
  return value.filterDeletedByUserId?.length ?? 0;
}

export function getDeletedByFiltersName(value: FiltersInternal) {
  if (value.filterDeletedByUserId?.length) {
    return <T keyName="trash_filter_deleted_by" />;
  }
}
