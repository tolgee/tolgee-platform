import { useState } from 'react';
import { useProject } from 'tg.hooks/useProject';
import { TaskCreateDialog } from 'tg.component/task/taskCreate/TaskCreateDialog';

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
  const languagesWithoutBase = allLanguages.filter((l) => !l.base);
  const selection = useTranslationsSelector((c) => c.selection);
  const translationsLanguages = useTranslationsSelector(
    (c) => c.translationsLanguages
  );

  return (
    <OperationContainer>
      <BatchOperationsSubmit
        disabled={disabled}
        onClick={() => setDialogOpen(true)}
      />
      <TaskCreateDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        selection={selection}
        initialLanguages={getPreselectedLanguagesIds(
          languagesWithoutBase,
          translationsLanguages ?? []
        )}
        allLanguages={allLanguages}
        project={project}
        onFinished={onFinished}
      />
    </OperationContainer>
  );
};
