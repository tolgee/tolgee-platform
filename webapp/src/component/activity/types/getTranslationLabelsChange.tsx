import { styled } from '@mui/material';

import { DiffValue } from '../types';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationLabel } from 'tg.component/TranslationLabel';

type LabelModel = components['schemas']['LabelModel'];

const StyledAdded = styled('span')`
  color: ${({ theme }) => theme.palette.activity.added};
  margin-right: 5px;
`;

const StyledRemoved = styled('span')`
  color: ${({ theme }) => theme.palette.activity.removed};
  margin-right: 5px;
`;

export const getTranslationLabelChange = (input?: DiffValue<LabelModel[]>) => {
  const oldInput = input?.old || [];
  const newInput = input?.new || [];

  const removed: LabelModel[] = [];
  const added: LabelModel[] = [];

  newInput.forEach((label: LabelModel) => {
    if (!oldInput.some((oldLabel) => oldLabel.id === label.id)) {
      added.push(label);
    }
  });

  oldInput.forEach((label) => {
    if (!newInput.some((newLabel) => newLabel.id === label.id)) {
      removed.push(label);
    }
  });

  return (
    <>
      {removed.map((label) => (
        <div key={label.id}>
          <StyledRemoved>-</StyledRemoved>
          <TranslationLabel key={label.id} label={label} />
        </div>
      ))}
      {added.map((label) => (
        <div key={label.id}>
          <StyledAdded>+</StyledAdded>
          <TranslationLabel key={label.id} label={label} />
        </div>
      ))}
    </>
  );
};
