import { useMemo, useState } from 'react';

import { useProject } from 'tg.hooks/useProject';
import { useTranslationsSelector } from 'tg.views/projects/translations/context/TranslationsContext';
import { OperationProps } from 'tg.views/projects/translations/BatchOperations/types';
import { BatchOperationsSubmit } from 'tg.views/projects/translations/BatchOperations/components/BatchOperationsSubmit';
import { OperationContainer } from 'tg.views/projects/translations/BatchOperations/components/OperationContainer';
import { BatchOperationsLanguagesSelect } from 'tg.views/projects/translations/BatchOperations/components/BatchOperationsLanguagesSelect';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useAiReviewRules } from 'tg.ee.module/aiReview/hooks/useAiReviewRules';
import { AiReviewRunDialog } from 'tg.ee.module/aiReview/components/AiReviewRunDialog';
import { getRuleCoveredLanguageIds } from 'tg.ee.module/aiReview/utils/ruleLanguages';

export const OperationAiReview = ({ disabled, onStart }: OperationProps) => {
  const project = useProject();
  const permissions = useProjectPermissions();
  const allLanguages = useTranslationsSelector((c) => c.languages) || [];
  const translationsLanguages = useTranslationsSelector(
    (c) => c.translationsLanguages
  );
  const selection = useTranslationsSelector((c) => c.selection);
  const rulesLoadable = useAiReviewRules(project.id);
  const [dialogOpen, setDialogOpen] = useState(false);

  const languages = allLanguages.filter((l) => !l.base);

  const preselected = useMemo(() => {
    const rules = rulesLoadable.data?._embedded?.rules;
    if (!rules) {
      return undefined;
    }
    const covered = getRuleCoveredLanguageIds(
      rules,
      languages.map((l) => l.id)
    );
    return languages
      .filter(
        (l) =>
          translationsLanguages?.includes(l.tag) &&
          covered.has(l.id) &&
          permissions.satisfiesLanguageAccess('translations.state-edit', l.id)
      )
      .map((l) => l.tag);
  }, [rulesLoadable.data, allLanguages, translationsLanguages]);

  const [userSelection, setUserSelection] = useState<string[]>();
  const selectedLangs = userSelection ?? preselected ?? [];

  const languageIds = allLanguages
    .filter((l) => selectedLangs.includes(l.tag))
    .map((l) => l.id);

  return (
    <OperationContainer>
      <BatchOperationsLanguagesSelect
        languages={languages}
        value={selectedLangs}
        onChange={setUserSelection}
        languagePermission="translations.state-edit"
      />
      <BatchOperationsSubmit
        disabled={disabled || selectedLangs.length === 0}
        onClick={() => setDialogOpen(true)}
      />
      {dialogOpen && (
        <AiReviewRunDialog
          projectId={project.id}
          variant="selection"
          request={{ keyIds: selection, languageIds }}
          keyCount={selection.length}
          languageCount={languageIds.length}
          onClose={() => setDialogOpen(false)}
          onStarted={(job) => {
            setDialogOpen(false);
            onStart(job);
          }}
        />
      )}
    </OperationContainer>
  );
};
