import React from 'react';

import { AppChips } from 'tg.component/apps/AppChips';
import { useAppScopeLabels } from 'tg.component/apps/useAppScopeLabels';

type Props = {
  scopes: string[];
  dataCy: string;
  color?: 'default' | 'info' | 'warning';
  variant?: 'filled' | 'outlined';
  label?: React.ReactNode;
  tooltip?: React.ReactNode;
  emptyLabel?: React.ReactNode;
  stacked?: boolean;
};

/** Permission scopes as chips, labeled with their human translations. */
export const AppScopeChips = ({ scopes, ...chipProps }: Props) => {
  const { getScopeLabel } = useAppScopeLabels();
  return (
    <AppChips
      items={scopes.map((scope) => ({
        id: scope,
        label: getScopeLabel(scope),
      }))}
      {...chipProps}
    />
  );
};
