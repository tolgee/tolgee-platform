import { Box, FormControl, InputLabel, MenuItem, Select } from '@mui/material';
import { T } from '@tolgee/react';
import { useField } from 'formik';
import { components } from 'tg.service/apiSchema.generated';

const OwnerSelect = (props: {
  organizations: components['schemas']['PagedModelOrganizationModel'];
}) => {
  const data = props.organizations._embedded?.organizations?.map((i) => ({
    value: i.id,
    key: i.id,
    render: <>{i.name}</>,
  })) || [
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
  const [fieldProps, _fieldMeta, fieldHelpers] = useField('organizationId');

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
          {items}
        </Select>
      </FormControl>
    </Box>
  );
};

export default OwnerSelect;
