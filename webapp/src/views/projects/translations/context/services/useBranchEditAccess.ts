import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useIsBranchingEnabled } from 'tg.component/branching/useIsBranchingEnabled';
import { useBranchesService } from './useBranchesService';
import { useTranslationsSelector } from '../TranslationsContext';

type Props = {
  projectId?: number;
};

export const useBranchEditAccess = ({ projectId }: Props = {}) => {
  const isBranchingEnabled = useIsBranchingEnabled();
  const { satisfiesPermission } = useProjectPermissions();
  const translationsSelected = useTranslationsSelector(
    (c) => c?.branches?.selected ?? null
  );
  const translationsLoadable = useTranslationsSelector(
    (c) => c?.branches?.loadable
  );
  const hasTranslationsContext = Boolean(translationsLoadable);

  const branchesService = useBranchesService({
    projectId,
    enabled: !hasTranslationsContext,
  });

  const selectedBranch = hasTranslationsContext
    ? translationsSelected
    : branchesService.selected;

  const isFetched = hasTranslationsContext
    ? Boolean(translationsLoadable?.isFetched)
    : branchesService.loadable.isFetched;

  if (!isBranchingEnabled) {
    return true;
  }

  if (!isFetched) {
    return true;
  }

  if (!selectedBranch) {
    return false;
  }

  if (!selectedBranch.isProtected) {
    return true;
  }

  return satisfiesPermission('branch.protected-modify');
};
