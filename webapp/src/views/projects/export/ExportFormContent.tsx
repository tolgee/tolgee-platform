import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { StateType } from 'tg.constants/translationStates';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { components } from 'tg.service/apiSchema.generated';

import { MessageFormat, getFormatById } from './components/formatGroups';
import { StateSelector } from './components/StateSelector';
import { LanguageSelector } from './components/LanguageSelector';
import { FormatSelector } from './components/FormatSelector';
import { SupportArraysSelector } from './components/SupportArraysSelector';
import { MessageFormatSelector } from './components/MessageFormatSelector';
import { NsSelector } from './components/NsSelector';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledOptions = styled('div')`
  display: grid;
`;

type FormValues = {
  states: StateType[];
  languages: string[];
  format: string;
  namespaces?: string[];
  nested: boolean;
  supportArrays: boolean;
  messageFormat: MessageFormat | undefined;
};

type Props = {
  values: FormValues;
  allLanguages: LanguageModel[];
  allNamespaces?: string[] | undefined;
  isSubmitting: boolean;
  isValid: boolean;
};

export const ExportFormContent = ({
  values,
  allLanguages,
  allNamespaces,
  isSubmitting,
  isValid,
}: Props) => {
  const { t } = useTranslate();
  return (
    <>
      <StateSelector className="states" />
      <LanguageSelector className="langs" languages={allLanguages} />
      <FormatSelector className="format" />
      {getFormatById(values.format).defaultSupportArrays && (
        <>
          <StyledOptions className="options">
            <SupportArraysSelector />
          </StyledOptions>
        </>
      )}
      <MessageFormatSelector className="messageFormat" />
      <NsSelector className="ns" namespaces={allNamespaces} />
      <div className="submit">
        <LoadingButton
          data-cy="export-submit-button"
          loading={isSubmitting}
          variant="contained"
          color="primary"
          type="submit"
          disabled={!isValid}
        >
          {t('export_translations_export_label')}
        </LoadingButton>
      </div>
    </>
  );
};
