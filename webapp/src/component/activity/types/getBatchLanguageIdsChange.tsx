import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { DiffValue } from '../types';
import { StyledReferences } from '../references/AnyReference';

type Props = {
  input: DiffValue<number[]>;
};

const LanguageIdsComponent: React.FC<Props> = ({ input }) => {
  const allLangs = useProjectLanguages();
  const newInput = input.new;
  if (newInput) {
    return (
      <StyledReferences>
        {input.new?.map((langId) => {
          const language = allLangs.find((lang) => lang.id === langId);
          return (
            <span key={langId} className="reference referenceComposed">
              {language && (
                <span className="referenceText">{language.name} </span>
              )}
              <CircledLanguageIcon flag={language?.flagEmoji} size={14} />
            </span>
          );
        })}
      </StyledReferences>
    );
  } else {
    return null;
  }
};

export const getBatchLanguageIdsChange = (input: DiffValue<number[]>) => {
  return <LanguageIdsComponent input={input} />;
};
