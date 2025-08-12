import { DialogTitle, styled } from '@mui/material';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { KeyCreateForm } from '../KeyCreateForm/KeyCreateForm';

type KeyWithDataModel = components['schemas']['KeyWithDataModel'];

const StyledContent = styled('div')`
  display: grid;
  row-gap: ${({ theme }) => theme.spacing(2)};
  margin-left: ${({ theme }) => theme.spacing(3)};
  margin-right: ${({ theme }) => theme.spacing(3)};
  margin-bottom: ${({ theme }) => theme.spacing(2)};
`;

type Props = {
  onClose: () => void;
  onDirtyChange: (dirty: boolean) => void;
};

export const KeyCreateDialog: React.FC<Props> = ({
  onClose,
  onDirtyChange,
}) => {
  const { insertTranslation } = useTranslationsActions();
  const baseLanguage = useTranslationsSelector((c) => c.baseLanguage);

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
        activeSuggestionCount: 0,
        totalSuggestionCount: 0,
        fromTranslationMemory: false,
        labels: [],
      };
    });

    insertTranslation({
      createdAt: Date.now(),
      keyId: data.id,
      keyNamespace: data.namespace,
      keyDescription: data.description,
      keyName: data.name,
      keyTags: data.tags,
      screenshotCount: 0,
      translations,
      contextPresent: false,
      keyIsPlural: data.isPlural,
      keyPluralArgName: data.pluralArgName,
    });
  };

  return (
    <>
      <DialogTitle>
        <T keyName="translation_single_create_title" />
      </DialogTitle>
      <StyledContent>
        <KeyCreateForm
          baseLanguage={baseLanguage}
          onSuccess={handleOnSuccess}
          onCancel={onClose}
          onDirtyChange={onDirtyChange}
          autofocus
        />
      </StyledContent>
    </>
  );
};
