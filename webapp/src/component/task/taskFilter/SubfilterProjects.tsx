import { useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';

import { SubmenuItem } from './SubmenuItem';
import { ProjectSearchSelectPopover } from 'tg.component/projectSearchSelect/ProjectSearchSelectPopover';

type Props = {
  value: number[];
  onChange: (value: number[]) => void;
};

export const SubfilterProjects = ({ value, onChange }: Props) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('task_filter_projects')}
        onClick={() => setOpen(true)}
        selected={Boolean(value?.length)}
      />
      {open && (
        <ProjectSearchSelectPopover
          open={open}
          onClose={() => setOpen(false)}
          anchorEl={anchorEl.current!}
          selected={value.map((id) => ({ id, name: '', username: '' }))}
          onSelectImmediate={(users) => onChange(users.map((u) => u.id))}
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
