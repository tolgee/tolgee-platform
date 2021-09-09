import { makeStyles } from '@material-ui/core';

import { LanguagesMenu } from 'tg.component/common/form/LanguagesMenu';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { useProject } from 'tg.hooks/useProject';
import { useUrlSearch } from 'tg.hooks/useUrlSearch.ts';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { KeyCreateForm } from './KeyCreateForm';
import { KeyEditForm } from './KeyEditForm';
import { LanguageType } from './TranslationsForm';

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'grid',
    rowGap: theme.spacing(2),
  },
  languagesMenu: {
    justifySelf: 'end',
  },
  label: {
    fontWeight: 'bold',
  },
}));

type Props = {
  exists: boolean;
  defaultSelectedLanguages: LanguageType[];
};

export const KeySingle: React.FC<Props> = ({
  exists,
  defaultSelectedLanguages,
}) => {
  const classes = useStyles();
  const project = useProject();
  const [languages, setLanguages] = useUrlSearchState('languages', {
    array: true,
  });

  const keyName = useUrlSearch().key as string;

  const translations = useApiQuery({
    url: '/v2/projects/{projectId}/translations',
    method: 'get',
    path: { projectId: project.id },
    query: { filterKeyName: keyName, languages: languages as string[] },
    options: { keepPreviousData: true, enabled: exists },
  });

  const selectedLanguages = (languages as string[]).length
    ? (languages as string[])
    : defaultSelectedLanguages.map((l) => l.tag) || [];

  const allLanguages = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
  });

  useGlobalLoading(translations.isFetching || allLanguages.isFetching);

  const translation = translations.data?._embedded?.keys?.[0];

  const selectedLanguagesMapped = selectedLanguages.map((l) => {
    const language = allLanguages.data?._embedded?.languages?.find(
      ({ tag }) => tag === l
    );
    return {
      name: language?.name || l,
      tag: l,
    };
  });

  return allLanguages.data && (!exists || translation) ? (
    <div className={classes.container}>
      <div className={classes.languagesMenu}>
        <LanguagesMenu
          languages={allLanguages.data!._embedded!.languages!.map(
            ({ tag, name }) => ({ value: tag, label: name })
          )}
          onChange={setLanguages}
          value={selectedLanguages}
          context="translation-single"
        />
      </div>
      {translation ? (
        <KeyEditForm
          translation={translation}
          languages={translations.data?.selectedLanguages || []}
        />
      ) : (
        <KeyCreateForm languages={selectedLanguagesMapped} />
      )}
    </div>
  ) : null;
};
