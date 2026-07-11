import React from 'react';
import { useTranslate } from '@tolgee/react';
import { LabelHint } from 'tg.component/common/LabelHint';
import { components } from 'tg.service/apiSchema.generated';

import {
  StyledLanguageTable,
  TABLE_TOP_ROW,
} from 'tg.component/languages/tableStyles';
import { AiLanguagesTableRow } from './AiLanguagesTableRow';

type LanguageModel = components['schemas']['LanguageModel'];
type LanguageAiPromptCustomizationModel =
  components['schemas']['LanguageAiPromptCustomizationModel'];

type Props = {
  languages: LanguageModel[];
  descriptions: LanguageAiPromptCustomizationModel[];
};

export const AiLanguagesTable = ({ languages, descriptions }: Props) => {
  const { t } = useTranslate();

  return (
    <StyledLanguageTable style={{ gridTemplateColumns: 'auto 1fr auto' }}>
      <div className={TABLE_TOP_ROW} />
      <div className={TABLE_TOP_ROW}>
        <LabelHint title={t('language_ai_prompts_row_hint')}>
          {t('language_ai_prompt_row_label')}
        </LabelHint>
      </div>
      <div className={TABLE_TOP_ROW} />

      {languages
        .filter(({ base }) => !base)
        .map((l) => (
          <React.Fragment key={l.id}>
            <AiLanguagesTableRow
              language={l}
              description={
                descriptions.find((d) => d.language.id === l.id)?.description
              }
            />
          </React.Fragment>
        ))}
    </StyledLanguageTable>
  );
};
