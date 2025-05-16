import { useState } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { TABLE_FIRST_CELL } from 'tg.component/languages/tableStyles';
import { LanguageItem } from 'tg.component/languages/LanguageItem';
import { IconButton, styled } from '@mui/material';
import { Plus, Edit02 } from '@untitled-ui/icons-react';
import { AiLanguageDescriptionDialog } from './AiLanguageDescriptionDialog';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledAction = styled('div')`
  padding: 0px 20px;
`;

const StyledNote = styled('div')`
  padding: 0px 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

type Props = {
  language: LanguageModel;
  description: string | undefined;
};

export const AiLanguagesTableRow = ({ language, description }: Props) => {
  const [dialogOpen, setDialogOpen] = useState(false);

  return (
    <>
      <div className={TABLE_FIRST_CELL} style={{ minWidth: 200 }}>
        <LanguageItem language={language} />
      </div>

      <StyledNote
        data-cy="ai-languages-description"
        data-cy-language={language.tag}
      >
        {description}
      </StyledNote>
      <StyledAction>
        <IconButton
          size="small"
          onClick={() => setDialogOpen(true)}
          data-cy="ai-languages-description-edit"
          data-cy-language={language.tag}
        >
          {description ? (
            <Edit02 width={19} height={19} />
          ) : (
            <Plus width={19} height={19} />
          )}
        </IconButton>
      </StyledAction>
      {dialogOpen && (
        <AiLanguageDescriptionDialog
          language={language}
          currentValue={description || ''}
          onClose={() => setDialogOpen(false)}
          placeholder='E.g. Use "delete" rather than "remove"'
        />
      )}
    </>
  );
};
