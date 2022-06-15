import { Box, FormControl, InputLabel, MenuItem, Select } from '@mui/material';
import { T } from '@tolgee/react';
import { useField } from 'formik';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { useUser } from 'tg.hooks/useUser';
import { useApiQuery } from 'tg.service/http/useQueryApi';

const OwnerSelect = () => {
  const user = useUser();

  const organizationsLoadable = useApiQuery({
    url: '/v2/organizations',
    method: 'get',
    query: {
      size: 100,
      params: {
        filterCurrentUserOwner: true,
      },
    },
  });

  const data = organizationsLoadable.data?._embedded?.organizations?.map(
    (i) => ({ value: i.id, key: i.id, render: <>i.name</> })
  ) || [
    {
      value: 0,
      key: 0,
      render: <T>project_create_select_owner_no_organization</T>,
    },
  ];
  const items = data.map((item) => (
    <MenuItem
      data-cy="project-owner-select-item"
      key={item.key}
      value={item.value}
    >
      {item.render}
    </MenuItem>
  ));
  const [fieldProps, _fieldMeta, fieldHelpers] = useField('owner');

  return (
    <Box mt={2}>
      <FormControl fullWidth={true} variant="standard">
        <InputLabel id="project-owner-select">
          <T>create_project_owner_label</T>
        </InputLabel>
        <Select
          data-cy="project-owner-select"
          labelId="project-owner-select"
          id="demo-simple-select"
          name="owner"
          value={fieldProps.value}
          onChange={(e) => fieldHelpers.setValue(e.target.value)}
          renderValue={(v) => {
            return data.find((i) => i.value === v)?.render;
          }}
        >
          {organizationsLoadable.isLoading ? <BoxLoading /> : items}
        </Select>
      </FormControl>
    </Box>
  );
};

export default OwnerSelect;
