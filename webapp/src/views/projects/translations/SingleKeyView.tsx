import { useProject } from 'tg.hooks/useProject';
import { useUrlSearch } from 'tg.hooks/useUrlSearch';
import { TranslationsContextProvider } from './context/TranslationsContext';
import { KeySingle } from './KeySingle/KeySingle';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';

export const SingleKeyView = () => {
  const project = useProject();
  const keyId = Number(useUrlSearch().id as string);
  const keyName = useUrlSearch().key as string;
  const keyNamespace = useUrlSearch().ns as string;

  const [aiPlayground] = useUrlSearchState('aiPlayground', {
    defaultVal: undefined,
    history: false,
  });

  return (
    <TranslationsContextProvider
      projectId={project.id}
      baseLang={project.baseLanguage?.tag}
      keyId={keyId}
      keyName={keyName}
      keyNamespace={keyNamespace}
      pageSize={1}
      aiPlayground={Boolean(aiPlayground)}
    >
      <KeySingle keyName={keyName} keyId={keyId} />
    </TranslationsContextProvider>
  );
};
