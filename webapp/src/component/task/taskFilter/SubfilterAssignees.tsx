import { useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { SubmenuItem } from './SubmenuItem';
import { AssigneeSearchSelectPopover } from '../assigneeSelect/AssigneeSearchSelectPopover';

type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

type Props = {
  value: number[];
  onChange: (value: number[]) => void;
  project: SimpleProjectModel;
};

export const SubfilterAssignees = ({ value, onChange, project }: Props) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('task_filter_assignees')}
        onClick={() => setOpen(true)}
        selected={Boolean(value?.length)}
      />
      {open && (
        <AssigneeSearchSelectPopover
          open={open}
          onClose={() => setOpen(false)}
          anchorEl={anchorEl.current!}
          selected={value.map((id) => ({ id, name: '', username: '' }))}
          onSelectImmediate={(users) => onChange(users.map((u) => u.id))}
          project={project}
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
