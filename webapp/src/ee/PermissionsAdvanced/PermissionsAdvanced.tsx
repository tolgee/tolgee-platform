import {
  HierarchyItem,
  LanguageModel,
  PermissionAdvancedState,
} from 'tg.component/PermissionsSettings/types';
import { Hierarchy } from './Hierarchy';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { PaidFeatureBanner } from '../common/PaidFeatureBanner';
import { usePermissionsStructure } from './usePermissionsStructure';

type Props = {
  dependencies: HierarchyItem;
  state: PermissionAdvancedState;
  onChange: (value: PermissionAdvancedState) => void;
  allLangs?: LanguageModel[];
};

export const PermissionsAdvanced: React.FC<Props> = ({
  dependencies,
  state,
  onChange,
  allLangs,
}) => {
  const { isEnabled } = useEnabledFeatures();

  if (!isEnabled('GRANULAR_PERMISSIONS')) {
    return <PaidFeatureBanner />;
  }

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
