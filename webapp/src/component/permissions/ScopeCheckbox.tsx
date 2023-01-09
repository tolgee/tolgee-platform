import { getRequirements, Scope } from './permissionHelper';
import { Checkbox } from '@mui/material';

export function ScopeCheckbox(props: {
  onChange: (event, checked) => void;
  selectedScopes: Scope[];
  scope: Scope;
}) {
  const isForceChecked = props.selectedScopes.some((it) =>
    getRequirements(it).includes(props.scope)
  );

  return (
    <Checkbox
      onChange={props.onChange}
      checked={props.selectedScopes.includes(props.scope) || isForceChecked}
      disabled={isForceChecked}
    />
  );
}
