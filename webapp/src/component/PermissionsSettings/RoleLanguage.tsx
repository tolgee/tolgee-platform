import { Box, Typography } from '@mui/material';
import { LanguagePermissionsMenu } from 'tg.component/security/LanguagePermissionsMenu';
import {
  getBlockingScopes,
  getMinimalLanguages,
  getScopeLanguagePermission,
  isAllLanguages,
  updateByDependencies,
} from 'tg.ee/PermissionsAdvanced/hierarchyTools';
import { useScopeTranslations } from 'tg.ee/PermissionsAdvanced/useScopeTranslations';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { HierarchyItem, PermissionState, PermissionModelScope } from './types';

type Props = {
  scope: PermissionModelScope;
  state: PermissionState;
  onChange: (value: PermissionState) => void;
  dependencies: HierarchyItem;
};

export const RoleLanguage: React.FC<Props> = ({
  scope,
  state,
  dependencies,
  onChange,
}) => {
  const languageProp = getScopeLanguagePermission(scope)!;
  const scopes = state.scopes;
  const languages = state[languageProp] || [];
  const allLangs = useProjectLanguages().map((l) => l.id);

  // check if all dependant scopes are in responsible nodes
  // meaning if we toggle this, nothing outside gets broken
  const blockingScopes = getBlockingScopes([scope], scopes, dependencies);
  const blockedLanguages = getMinimalLanguages(blockingScopes, state, allLangs);

  const { getScopeTranslation } = useScopeTranslations();

  const handleSelect = (values: number[]) => {
    const { scopes: _, ...other } = updateByDependencies(
      [scope],
      {
        ...state,
        scopes,
        [languageProp]: values,
      },
      dependencies,
      allLangs
    );
    const newState = {
      ...other,
      role: state.role,
      scopes: state.scopes,
    };
    onChange(newState);
  };

  return (
    <Box display="grid" gridAutoFlow="row" minWidth="200px">
      <Typography variant="caption">{getScopeTranslation(scope)}</Typography>
      <LanguagePermissionsMenu
        selected={
          !blockedLanguages
            ? languages
            : isAllLanguages(blockedLanguages, allLangs) &&
              isAllLanguages(languages, allLangs)
            ? []
            : languages
        }
        onSelect={handleSelect}
        disabled={blockedLanguages}
      />
    </Box>
  );
};
