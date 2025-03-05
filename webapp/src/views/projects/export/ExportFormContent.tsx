import { styled } from '@mui/material';
import { StateType } from 'tg.constants/translationStates';
import { components } from 'tg.service/apiSchema.generated';

import { MessageFormat, getFormatById } from './components/formatGroups';
import { StateSelector } from './components/StateSelector';
import { LanguageSelector } from './components/LanguageSelector';
import { FormatSelector } from './components/FormatSelector';
import { SupportArraysSelector } from './components/SupportArraysSelector';
import { MessageFormatSelector } from './components/MessageFormatSelector';
import { NsSelector } from './components/NsSelector';
import { EscapeHtmlSelector } from './components/EscapeHtmlSelector';

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
  escapeHtml: boolean;
  messageFormat: MessageFormat | undefined;
};

type Props = {
  values: FormValues;
  allLanguages: LanguageModel[];
  allNamespaces?: string[] | undefined;
};

export const ExportFormContent = ({
  values,
  allLanguages,
  allNamespaces,
}: Props) => {
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
      {getFormatById(values.format).showEscapeHtml && (
        <StyledOptions className="options">
          <EscapeHtmlSelector />
        </StyledOptions>
      )}
      <MessageFormatSelector className="messageFormat" />
      <NsSelector className="ns" namespaces={allNamespaces} />
    </>
  );
};
