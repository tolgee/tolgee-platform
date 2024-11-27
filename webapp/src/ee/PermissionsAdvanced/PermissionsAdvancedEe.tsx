import { Hierarchy } from './Hierarchy';
import { usePermissionsStructure } from './usePermissionsStructure';
import { PermissionsAdvancedEeProps } from '../../plugin/PluginType';
import { FC } from 'react';

export const PermissionsAdvancedEe: FC<PermissionsAdvancedEeProps> = ({
  dependencies,
  state,
  onChange,
  allLangs,
}) => {
  const structure = usePermissionsStructure();

  return (
    <Hierarchy
      dependencies={dependencies}
      state={state}
      onChange={onChange}
      allLangs={allLangs}
      structure={structure}
    />
  );
};
