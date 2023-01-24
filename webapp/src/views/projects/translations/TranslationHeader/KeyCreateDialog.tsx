import { DialogTitle, styled } from '@mui/material';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import {
  useTranslationsSelector,
  useTranslationsActions,
} from '../context/TranslationsContext';
import { KeyCreateForm } from '../KeyCreateForm/KeyCreateForm';

type KeyWithDataModel = components['schemas']['KeyWithDataModel'];

const StyledTitle = styled('div')`
  justify-self: stretch;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
`;

const StyledContent = styled('div')`
  display: grid;
  row-gap: ${({ theme }) => theme.spacing(2)};
  margin-left: ${({ theme }) => theme.spacing(3)};
  margin-right: ${({ theme }) => theme.spacing(3)};
  margin-bottom: ${({ theme }) => theme.spacing(2)};
`;

type Props = {
  onClose: () => void;
};

export const KeyCreateDialog: React.FC<Props> = ({ onClose }) => {
  const { insertTranslation } = useTranslationsActions();

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

    insertTranslation({
      keyId: data.id,
      keyNamespace: data.namespace,
      keyName: data.name,
      keyTags: data.tags,
      screenshotCount: 0,
      translations,
    });
  };

  return (
    <>
      <DialogTitle>
        <StyledTitle>
          <T>translation_single_create_title</T>
          <LanguagesSelect
            languages={languages || []}
            value={selectedLanguages}
            onChange={handleLanguageChange}
            context="translations-dialog"
          />
        </StyledTitle>
      </DialogTitle>
      <StyledContent>
        <KeyCreateForm
          languages={selectedLanguagesMapped!}
          onSuccess={handleOnSuccess}
          onCancel={onClose}
          autofocus
        />
      </StyledContent>
    </>
  );
};
