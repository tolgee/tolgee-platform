import { Hierarchy } from 'tg.component/PermissionsSettings/Hierarchy';
import { usePermissionsStructure } from 'tg.component/PermissionsSettings/usePermissionsStructure';
import { PermissionsAdvancedEeProps } from '../../eeSetup/EeModuleType';
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
