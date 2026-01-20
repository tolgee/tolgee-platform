import React from 'react';
import { Box } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { useHistory, useLocation } from 'react-router-dom';
import { BranchSelect } from './BranchSelect';
import { useProject } from 'tg.hooks/useProject';
import { useBranchesService } from 'tg.views/projects/translations/context/services/useBranchesService';
import {
  applyBranchToLocation,
  extractBranchFromPathname,
} from './branchingPath';
import { setCachedBranch } from './branchCache';

type BranchModel = components['schemas']['BranchModel'];

export const GlobalBranchSelector = () => {
  const history = useHistory();
  const location = useLocation();
  const project = useProject();
  const branchInUrl = extractBranchFromPathname(location.pathname);
  const {
    selected,
    loadable,
    default: defaultBranch,
  } = useBranchesService({
    projectId: project.id,
    branchName: branchInUrl,
  });

  if (!loadable.isLoading && !selected) {
    history.replace(applyBranchToLocation(location, defaultBranch?.name));
    setCachedBranch(
      project.id,
      defaultBranch?.isDefault ? null : defaultBranch?.name || null
    );
  } else if (!loadable.isLoading && !branchInUrl) {
    // no branch in URL means user wants default/unbranched state
    setCachedBranch(project.id, null);
  }

  function changeBranch(item: BranchModel) {
    history.replace(applyBranchToLocation(location, item.name));
    setCachedBranch(project.id, item.isDefault ? null : item.name);
  }

  return (
    <Box display="grid">
      <BranchSelect branch={selected} onSelect={changeBranch} />
    </Box>
  );
};
