import { useProject } from 'tg.hooks/useProject';
import { useUrlSearch } from 'tg.hooks/useUrlSearch';
import { TranslationsContextProvider } from './context/TranslationsContext';
import { KeySingle } from './KeySingle/KeySingle';

export const SingleKeyView = () => {
  const project = useProject();
  const keyName = useUrlSearch().key as string;
  const keyNamespace = (useUrlSearch().ns as string) || '';
  const keyId = Number(useUrlSearch().id as string);

  return (
    <TranslationsContextProvider
      projectId={project.id}
      baseLang={project.baseLanguage?.tag}
      keyId={keyId}
      keyName={keyName}
      keyNamespace={keyNamespace}
      pageSize={1}
    >
      <KeySingle keyName={keyName} keyId={keyId} />
    </TranslationsContextProvider>
  );
};
