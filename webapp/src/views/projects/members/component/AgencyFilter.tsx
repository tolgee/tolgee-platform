import { useRef } from 'react';
import { useTranslate } from '@tolgee/react';
import { Checkbox, MenuItem, styled } from '@mui/material';

import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { AgencyLabel } from 'tg.ee';
import { Select } from 'tg.component/common/Select';

const StyledPlaceholder = styled('span')`
  opacity: 0.5;
  font-size: 15px;
`;

const StyledInputContent = styled('div')`
  height: 23px;
  text-wrap: nowrap;
  display: flex;
  gap: 8px;
  align-items: center;
  overflow: hidden;
  max-width: 150px;
`;

type Props = {
  value: number[];
  onChange: (value: number[]) => void;
};

export const AgencyFilter = ({ value, onChange }: Props) => {
  const agenciesLoadable = useBillingApiQuery({
    url: '/v2/billing/translation-agency',
    method: 'get',
    query: {
      size: 1000,
    },
  });

  const { t } = useTranslate();
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
    <Select
      ref={anchorEl as any}
      sx={{ width: 200 }}
      minHeight={false}
      multiple
      value={value ?? []}
      placeholder="test"
      displayEmpty
      renderValue={(value) => (
        <StyledInputContent>
          {(value as number[]).length ? (
            (value as number[])
              .map((aId) => data?.find((a) => a.id === aId))
              .filter(Boolean)
              .map((a) => <AgencyLabel key={a!.id} agency={a!} />)
          ) : (
            <StyledPlaceholder>{t('filter_by_agency')}</StyledPlaceholder>
          )}
        </StyledInputContent>
      )}
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
    </Select>
  );
};
