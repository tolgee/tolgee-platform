import { useState } from 'react';
import { useProject } from 'tg.hooks/useProject';
import { User } from 'tg.component/UserAccount';

import { OperationProps } from 'tg.views/projects/translations/BatchOperations/types';
import { BatchOperationsSubmit } from 'tg.views/projects/translations/BatchOperations/components/BatchOperationsSubmit';
import { OperationContainer } from 'tg.views/projects/translations/BatchOperations/components/OperationContainer';
import { useTranslationsSelector } from 'tg.views/projects/translations/context/TranslationsContext';
import { getPreselectedLanguagesIds } from 'tg.views/projects/translations/BatchOperations/getPreselectedLanguages';
import { TaskCreateDialog } from '../task/components/taskCreate/TaskCreateDialog';

type Props = OperationProps;

export const OperationTaskCreate = ({ disabled, onFinished }: Props) => {
  const project = useProject();
  const [dialogOpen, setDialogOpen] = useState(true);

  const allLanguages = useTranslationsSelector((c) => c.languages) ?? [];
  const selection = useTranslationsSelector((c) => c.selection);
  const translationsLanguages = useTranslationsSelector(
    (c) => c.translationsLanguages
  );

  const languageAssignees = {} as Record<number, User[]>;
  const selectedLanguages = getPreselectedLanguagesIds(
    allLanguages.filter((l) => !l.base),
    translationsLanguages ?? []
  );

  selectedLanguages.forEach((langId) => {
    languageAssignees[langId] = [];
  });

  return (
    <OperationContainer>
      <BatchOperationsSubmit
        disabled={disabled}
        onClick={() => setDialogOpen(true)}
      />
      <TaskCreateDialog
        key={JSON.stringify(selectedLanguages)}
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        initialValues={{
          selection,
          languageAssignees,
          languages: selectedLanguages,
        }}
        allLanguages={allLanguages}
        projectId={project.id}
        onFinished={onFinished}
      />
    </OperationContainer>
  );
};
