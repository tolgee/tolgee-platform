import { useRef, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Menu } from '@mui/material';
import { SubmenuItem } from 'tg.component/SubmenuItem';
import { UserAccount } from 'tg.component/UserAccount';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { FiltersInternal, FilterActions } from './tools';
import { FilterItem } from './FilterItem';

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

  const deletersLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/keys/trash/deleters',
    method: 'get',
    path: { projectId },
    query: {},
    options: {
      enabled: open,
      keepPreviousData: true,
    },
  });

  const users = deletersLoadable.data?._embedded?.users ?? [];

  const handleToggle = (userId: number) => {
    const isSelected = selectedIds.includes(userId);
    const newIds = isSelected
      ? selectedIds.filter((id) => id !== userId)
      : [...selectedIds, userId];

    actions.setFilters({
      ...value,
      filterDeletedByUserId: newIds.length ? newIds : undefined,
    });
  };

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
          onClose={() => setOpen(false)}
        >
          {users.map((user) => (
            <FilterItem
              key={user.id}
              label={<UserAccount user={user} />}
              selected={selectedIds.includes(user.id)}
              onClick={() => handleToggle(user.id)}
            />
          ))}
        </Menu>
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
