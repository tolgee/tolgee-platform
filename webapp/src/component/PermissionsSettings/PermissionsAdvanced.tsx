import { HierarchyItem, LanguageModel, PermissionAdvancedState } from './types';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { DisabledFeatureBanner } from '../common/DisabledFeatureBanner';
import { getEe } from '../../plugin/getEe';

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
  const { PermissionsAdvanced: PermissionsAdvancedEe } = getEe();

  const { isEnabled } = useEnabledFeatures();

  if (!isEnabled('GRANULAR_PERMISSIONS')) {
    return <DisabledFeatureBanner />;
  }
  return (
    <PermissionsAdvancedEe
      dependencies={dependencies}
      state={state}
      onChange={onChange}
      allLangs={allLangs}
    />
  );
};
