import { useState } from 'react';
import { useProject } from 'tg.hooks/useProject';
import { User } from 'tg.component/UserAccount';
import { TaskCreateDialog } from 'tg.ee/task/components/taskCreate/TaskCreateDialog';

import { OperationProps } from './types';
import { BatchOperationsSubmit } from './components/BatchOperationsSubmit';
import { OperationContainer } from './components/OperationContainer';
import { useTranslationsSelector } from '../context/TranslationsContext';
import { getPreselectedLanguagesIds } from './getPreselectedLanguages';

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
