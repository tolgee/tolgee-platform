import React, { FunctionComponent, useEffect, useState } from 'react';
import { Grid } from '@mui/material';

import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';

import { ImportConflictTranslation } from './ImportConflictTranslation';
import { useApiMutation } from 'tg.service/http/useQueryApi';

export const ImportConflictTranslationsPair: FunctionComponent<{
  translation: components['schemas']['ImportTranslationModel'];
  languageId: number;
}> = ({ translation, languageId }) => {
  const project = useProject();
  const [expanded, setExpanded] = useState(false);
  const [leftExpandable, setLeftExpandable] = useState(false);
  const [rightExpandable, setRightExpandable] = useState(false);
  const [keepLoaded, setKeepLoaded] = useState(false);
  const [overrideLoaded, setOverrideLoaded] = useState(false);

  const setOverrideMutation = useApiMutation({
    url: '/v2/projects/{projectId}/import/result/languages/{languageId}/translations/{translationId}/resolve/set-override',
    method: 'put',
    invalidatePrefix:
      '/v2/projects/{projectId}/import/result/languages/{languageId}',
  });

  const setKeepMutation = useApiMutation({
    url: '/v2/projects/{projectId}/import/result/languages/{languageId}/translations/{translationId}/resolve/set-keep-existing',
    method: 'put',
    invalidatePrefix:
      '/v2/projects/{projectId}/import/result/languages/{languageId}',
  });

  const setOverride = (translationId: number) => {
    setOverrideMutation.mutate({
      path: {
        projectId: project.id,
        languageId: languageId,
        translationId: translationId,
      },
    });
  };

  const setKeepExisting = (translationId: number) => {
    setKeepMutation.mutate({
      path: {
        projectId: project.id,
        languageId: languageId,
        translationId: translationId,
      },
    });
  };

  useEffect(() => {
    if (setKeepMutation.isSuccess) {
      setKeepLoaded(true);
    }
    setTimeout(() => setKeepLoaded(false), 300);
  }, [setKeepMutation.isSuccess]);

  useEffect(() => {
    if (setOverrideMutation.isSuccess) {
      setOverrideLoaded(true);
    }
    setTimeout(() => setOverrideLoaded(false), 300);
  }, [setOverrideMutation.isSuccess]);

  return (
    <>
      <Grid item lg md sm={12} xs={12} zeroMinWidth>
        <ImportConflictTranslation
          data-cy="import-resolution-dialog-existing-translation"
          loading={setKeepMutation.isLoading}
          loaded={keepLoaded}
          text={translation.conflictText!}
          selected={!translation.override && translation.resolved}
          onSelect={() => setKeepExisting(translation.id)}
          onToggle={() => setExpanded(!expanded)}
          expanded={expanded}
          onDetectedExpandability={setLeftExpandable}
          expandable={leftExpandable || rightExpandable}
        />
      </Grid>
      <Grid item lg md sm={12} xs={12} zeroMinWidth>
        <ImportConflictTranslation
          data-cy="import-resolution-dialog-new-translation"
          loading={setOverrideMutation.isLoading}
          loaded={overrideLoaded}
          text={translation.text}
          selected={
            (translation.override && translation.resolved) ||
            !translation.conflictId
          }
          onSelect={() => setOverride(translation.id)}
          onToggle={() => setExpanded(!expanded)}
          expanded={expanded}
          onDetectedExpandability={setRightExpandable}
          expandable={leftExpandable || rightExpandable}
        />
      </Grid>
    </>
  );
};
