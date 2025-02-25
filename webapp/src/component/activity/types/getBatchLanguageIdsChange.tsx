import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { DiffValue } from '../types';
import { StyledReferences } from '../references/AnyReference';
import { useTranslate } from '@tolgee/react';
import { Box, Tooltip } from '@mui/material';

type Props = {
  input: DiffValue<number[]>;
};

const DISPLAY_MAX_ITEMS = 3;

const LanguageIdsComponent: React.FC<Props> = ({ input }) => {
  const allLangs = useProjectLanguages();
  const newInput = input.new;

  let displayed = input.new || [];
  let other: number[] = [];

  if (displayed?.length > DISPLAY_MAX_ITEMS) {
    displayed = input.new?.slice(0, DISPLAY_MAX_ITEMS - 1) || [];
    other = input.new?.slice(DISPLAY_MAX_ITEMS - 1) || [];
  }

  const { t } = useTranslate();
  if (newInput) {
    return (
      <StyledReferences>
        {displayed?.map((langId) => {
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
        {Boolean(other?.length) && (
          <Tooltip
            title={
              <Box display="grid" gap="1px" fontSize={15}>
                {other?.map((langId) => {
                  const language = allLangs.find((lang) => lang.id === langId);
                  return (
                    <StyledReferences key={langId}>
                      <span className="reference referenceComposed">
                        {language && (
                          <span className="referenceText">
                            {language.name}{' '}
                          </span>
                        )}
                        <CircledLanguageIcon
                          flag={language?.flagEmoji}
                          size={14}
                        />
                      </span>
                    </StyledReferences>
                  );
                })}
              </Box>
            }
          >
            <span className="reference referenceComposed">
              <span className="referenceText">
                {t('activity_batch_more_items', { value: other!.length })}
              </span>
            </span>
          </Tooltip>
        )}
      </StyledReferences>
    );
  } else {
    return null;
  }
};

export const getBatchLanguageIdsChange = (input: DiffValue<number[]>) => {
  return <LanguageIdsComponent input={input} />;
};
