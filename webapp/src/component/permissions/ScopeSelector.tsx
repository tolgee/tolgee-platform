import { useState } from 'react';
import { Scope } from './permissionHelper';
import { ScopeCheckbox } from './ScopeCheckbox';
import { useTranslate } from '@tolgee/react';
import { getScopeCategories } from 'tg.constants/scopeCategories';
import { Typography } from '@mui/material';

export const ScopeSelector = () => {
  const [selectedScopes, setSelectedScopes] = useState([] as Scope[]);

  const t = useTranslate();
  const categories = getScopeCategories(t);

  function onChange(scope: Scope, checked: boolean) {
    if (checked) {
      setSelectedScopes([...selectedScopes, scope]);
    } else {
      setSelectedScopes(selectedScopes.filter((it) => it !== scope));
    }
  }

  return categories.map((category) => (
    <>
      <Typography variant="h6">{category.name}</Typography>
      {category.scopes.map((scope) => (
        <div key={scope}>
          {scope}
          <ScopeCheckbox
            onChange={(_, checked) => onChange(scope, checked)}
            selectedScopes={selectedScopes}
            scope={scope}
          />
        </div>
      ))}
    </>
  ));
};
