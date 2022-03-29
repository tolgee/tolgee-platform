import { useProject } from 'tg.hooks/useProject';
import { useUrlSearch, useUrlSearchArray } from 'tg.hooks/useUrlSearch';
import { TranslationsContextProvider } from './context/TranslationsContext';
import { KeySingle } from './KeySingle/KeySingle';

export const SingleKeyView = () => {
  const project = useProject();
  const keyName = useUrlSearch().key as string;
  const keyId = Number(useUrlSearch().id as string);
  const languages = useUrlSearchArray().languages;

  return (
    <TranslationsContextProvider
      projectId={project.id}
      baseLang={project.baseLanguage?.tag}
      keyId={keyId}
      keyName={keyName}
      languages={languages}
      pageSize={1}
    >
      <KeySingle keyName={keyName} keyId={keyId} />
    </TranslationsContextProvider>
  );
};
