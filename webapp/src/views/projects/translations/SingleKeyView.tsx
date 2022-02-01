import { useProject } from 'tg.hooks/useProject';
import { useUrlSearch, useUrlSearchArray } from 'tg.hooks/useUrlSearch.ts';
import { TranslationsContextProvider } from './context/TranslationsContext';
import { KeySingle } from './KeySingle/KeySingle';

export const SingleKeyView = () => {
  const project = useProject();
  const keyName = useUrlSearch().key as string;
  const languages = useUrlSearchArray().languages;

  return (
    <TranslationsContextProvider
      projectId={project.id}
      baseLang={project.baseLanguage?.tag}
      keyName={keyName}
      languages={languages}
      pageSize={1}
    >
      <KeySingle keyName={keyName} />
    </TranslationsContextProvider>
  );
};
