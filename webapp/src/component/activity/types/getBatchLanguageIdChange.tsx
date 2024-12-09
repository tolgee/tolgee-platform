import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { DiffValue } from '../types';
import { StyledReferences } from '../references/AnyReference';

type Props = {
  input: DiffValue<number>;
};

const LanguageIdsComponent: React.FC<Props> = ({ input }) => {
  const allLangs = useProjectLanguages();
  const langId = input.new;
  const language = allLangs.find((lang) => lang.id === langId);
  if (language) {
    return (
      <StyledReferences>
        <span className="reference referenceComposed">
          <span className="referenceText">{language.name} </span>
          <CircledLanguageIcon flag={language?.flagEmoji} size={14} />
        </span>
      </StyledReferences>
    );
  } else {
    return null;
  }
};

export const getBatchLanguageIdChange = (input: DiffValue<number>) => {
  return <LanguageIdsComponent input={input} />;
};
