import React from 'react';
import { Box } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { useHistory } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { BranchSelect } from 'tg.component/branching/BranchSelect';
import { useBranchesService } from 'tg.views/projects/translations/context/services/useBranchesService';

type BranchModel = components['schemas']['BranchModel'];

export const BranchSelector = () => {
  const project = useProject();
  const history = useHistory();
  const { selected, loadable } = useBranchesService({ projectId: project.id });

  if (!loadable.isLoading && !selected) {
    history.replace(
      LINKS.PROJECT_TRANSLATIONS.build({
        [PARAMS.PROJECT_ID]: project.id,
      })
    );
  }

  function changeBranch(item: BranchModel) {
    history.replace(
      LINKS.PROJECT_TRANSLATIONS_BRANCHED.build({
        [PARAMS.PROJECT_ID]: project.id,
        [PARAMS.TRANSLATIONS_BRANCH]: item.name,
      })
    );
  }

  return (
    <Box display="grid">
      <BranchSelect branch={selected} onSelect={changeBranch} />
    </Box>
  );
};
