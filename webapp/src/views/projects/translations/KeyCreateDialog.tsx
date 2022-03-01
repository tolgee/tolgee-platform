import { DialogTitle, makeStyles } from '@material-ui/core';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { LanguagesMenu } from 'tg.component/common/form/LanguagesMenu';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import {
  useTranslationsSelector,
  useTranslationsDispatch,
} from './context/TranslationsContext';
import { KeyCreateForm } from './KeyCreateForm/KeyCreateForm';

type KeyWithDataModel = components['schemas']['KeyWithDataModel'];

const useStyles = makeStyles((theme) => ({
  title: {
    justifySelf: 'stretch',
    display: 'flex',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
  },
  content: {
    display: 'grid',
    rowGap: theme.spacing(2),
    marginLeft: theme.spacing(3),
    marginRight: theme.spacing(3),
    marginBottom: theme.spacing(2),
  },
}));

type Props = {
  onClose: () => void;
};

export const KeyCreateDialog: React.FC<Props> = ({ onClose }) => {
  const classes = useStyles();
  const dispatch = useTranslationsDispatch();

  const languages = useTranslationsSelector((c) => c.languages);
  const selectedLanguagesDefault = useTranslationsSelector(
    (c) => c.selectedLanguages
  );

  const handleLanguageChange = (langs: string[]) => {
    setUrlSelectedLanguages(langs);
  };

  const [urlSelectedLanguages, setUrlSelectedLanguages] = useUrlSearchState(
    'languages',
    {
      array: true,
      cleanup: true,
    }
  );

  const selectedLanguages =
    (urlSelectedLanguages?.length && (urlSelectedLanguages as string[])) ||
    selectedLanguagesDefault ||
    [];

  const selectedLanguagesMapped = selectedLanguages!.map((l) => {
    const language = languages?.find(({ tag }) => tag === l);
    return language!;
  });

  const handleOnSuccess = (data: KeyWithDataModel) => {
    onClose();
    const translations: {
      [key: string]: components['schemas']['TranslationViewModel'];
    } = {};

    Object.entries(data.translations).forEach(([key, value]) => {
      translations[key] = {
        ...value,
        commentCount: 0,
        unresolvedCommentCount: 0,
        fromTranslationMemory: false,
      };
    });

    dispatch({
      type: 'INSERT_TRANSLATION',
      payload: {
        keyId: data.id,
        keyName: data.name,
        keyTags: data.tags,
        screenshotCount: 0,
        translations,
      },
    });
  };

  return (
    <>
      <DialogTitle>
        <div className={classes.title}>
          <T>translation_single_create_title</T>
          <LanguagesMenu
            languages={languages || []}
            value={selectedLanguages}
            onChange={handleLanguageChange}
            context="translations-dialog"
          />
        </div>
      </DialogTitle>
      <div className={classes.content}>
        <KeyCreateForm
          languages={selectedLanguagesMapped!}
          onSuccess={handleOnSuccess}
          onCancel={onClose}
          autofocus
        />
      </div>
    </>
  );
};
