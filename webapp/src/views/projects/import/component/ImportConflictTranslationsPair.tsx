import { FunctionComponent, useState } from 'react';
import { Grid } from '@mui/material';

import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { importActions } from 'tg.store/project/ImportActions';

import { ImportConflictTranslation } from './ImportConflictTranslation';

export const ImportConflictTranslationsPair: FunctionComponent<{
  translation: components['schemas']['ImportTranslationModel'];
  languageId: number;
}> = ({ translation, languageId }) => {
  const project = useProject();
  const [expanded, setExpanded] = useState(false);
  const [leftExpandable, setLeftExpandable] = useState(false);
  const [rightExpandable, setRightExpandable] = useState(false);

  const setOverride = (translationId: number) => {
    importActions.loadableActions.resolveTranslationConflictOverride.dispatch({
      path: {
        projectId: project.id,
        languageId: languageId,
        translationId: translationId,
      },
    });
  };

  const setKeepExisting = (translationId: number) => {
    importActions.loadableActions.resolveTranslationConflictKeep.dispatch({
      path: {
        projectId: project.id,
        languageId: languageId,
        translationId: translationId,
      },
    });
  };

  const isKeepExistingLoading = importActions.useSelector(
    (s) =>
      s.loadables.resolveTranslationConflictKeep.dispatchParams?.[0].path
        .translationId === translation.id &&
      s.loadables.resolveTranslationConflictKeep.loading
  );

  const isKeepExistingLoaded = importActions.useSelector(
    (s) =>
      s.loadables.resolveTranslationConflictKeep.dispatchParams?.[0].path
        .translationId === translation.id &&
      s.loadables.resolveTranslationConflictKeep.loaded
  );

  const isOverrideLoading = importActions.useSelector(
    (s) =>
      s.loadables.resolveTranslationConflictOverride.dispatchParams?.[0].path
        .translationId === translation.id &&
      s.loadables.resolveTranslationConflictOverride.loading
  );

  const isOverrideLoaded = importActions.useSelector(
    (s) =>
      s.loadables.resolveTranslationConflictOverride.dispatchParams?.[0].path
        .translationId === translation.id &&
      s.loadables.resolveTranslationConflictOverride.loaded
  );

  return (
    <>
      <Grid item lg md sm={12} xs={12} zeroMinWidth>
        <ImportConflictTranslation
          data-cy="import-resolution-dialog-existing-translation"
          loading={isKeepExistingLoading}
          loaded={isKeepExistingLoaded}
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
          loading={isOverrideLoading}
          loaded={isOverrideLoaded}
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
