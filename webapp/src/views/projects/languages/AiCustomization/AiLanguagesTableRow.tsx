import { useState } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { TABLE_FIRST_CELL } from '../tableStyles';
import { LanguageItem } from '../LanguageItem';
import { IconButton, styled } from '@mui/material';
import { Add, Edit } from '@mui/icons-material';
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
          {description ? <Edit fontSize="small" /> : <Add fontSize="small" />}
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
