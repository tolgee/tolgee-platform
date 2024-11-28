import { FunctionComponent, useMemo } from 'react';
import {
  FormControlProps,
  FormGroup,
  FormHelperText,
  FormLabel,
} from '@mui/material';
import { useField } from 'formik';
import { Hierarchy } from './Hierarchy';
import {
  limitStructureToOptions,
  usePermissionsStructure,
} from './usePermissionsStructure';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { PermissionAdvancedState } from './types';
import { SpinnerProgress } from '../SpinnerProgress';

interface CheckBoxGroupMultiSelectProps {
  name: string;
  label?: string;
  color?: 'primary' | 'secondary' | 'default';
  options: Set<string>;
}

type Props = CheckBoxGroupMultiSelectProps & FormControlProps;

export const CheckBoxGroupMultiSelect: FunctionComponent<Props> = (props) => {
  const [field, meta, helpers] = useField<Set<string>>(props.name);

  const structure = usePermissionsStructure();

  const limitedStructure = useMemo(
    () =>
      limitStructureToOptions([structure], Array.from(props.options) as any),
    [props.options]
  );

  const dependenciesLoadable = useApiQuery({
    url: '/v2/public/scope-info/hierarchy',
    method: 'get',
    query: {},
  });

  const onChange = (data: PermissionAdvancedState) => {
    const newValue = new Set(data.scopes);
    helpers.setValue(newValue);
  };

  return (
    <FormGroup data-cy="checkbox-group-multiselect">
      <FormLabel error={!!meta.error} component="legend">
        {props.label}
      </FormLabel>
      {dependenciesLoadable.isLoading ? (
        <SpinnerProgress />
      ) : (
        limitedStructure.map((structureItem, i) => (
          <Hierarchy
            key={i}
            structure={structureItem}
            dependencies={dependenciesLoadable.data!}
            state={{ scopes: Array.from(field.value) as any }}
            onChange={onChange}
          />
        ))
      )}
      {!!meta.error && (
        <FormHelperText error={!!meta.error}>{meta.error}</FormHelperText>
      )}
    </FormGroup>
  );
};
