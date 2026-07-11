import { useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Checkbox, Menu, MenuItem } from '@mui/material';

import { SubmenuItem } from '../../../../component/SubmenuItem';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { AgencyLabel } from 'tg.ee';

type Props = {
  value: number[];
  onChange: (value: number[]) => void;
};

export const SubfilterAgencies = ({ value, onChange }: Props) => {
  const agenciesLoadable = useBillingApiQuery({
    url: '/v2/billing/translation-agency',
    method: 'get',
    query: {
      size: 1000,
    },
  });

  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);
  const handleAgencyToggle = (id: number) => () => {
    if (value.includes(id)) {
      onChange(value.filter((l) => l !== id));
    } else {
      onChange([...value, id]);
    }
  };

  const data = agenciesLoadable.data?._embedded?.translationAgencies;

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('task_filter_agencies')}
        onClick={() => setOpen(true)}
        selected={Boolean(value?.length)}
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
          data-cy="language-select-popover"
        >
          {data?.map((agency) => (
            <MenuItem
              key={agency.id}
              value={agency.id}
              onClick={handleAgencyToggle(agency.id)}
            >
              <Checkbox
                checked={value?.includes(agency.id)}
                size="small"
                edge="start"
                disableRipple
              />
              <AgencyLabel agency={agency} />
            </MenuItem>
          ))}
        </Menu>
      )}
    </>
  );
};
