import { useState } from 'react';
import { useProject } from 'tg.hooks/useProject';
import { User } from 'tg.component/UserAccount';
import { OrderTranslationsDialog } from 'tg.ee';

import { OperationProps } from './types';
import { BatchOperationsSubmit } from './components/BatchOperationsSubmit';
import { OperationContainer } from './components/OperationContainer';
import { useTranslationsSelector } from '../context/TranslationsContext';
import { getPreselectedLanguagesIds } from './getPreselectedLanguages';

type Props = OperationProps;

export const OperationOrderTranslation = ({ disabled, onFinished }: Props) => {
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
      <OrderTranslationsDialog
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
