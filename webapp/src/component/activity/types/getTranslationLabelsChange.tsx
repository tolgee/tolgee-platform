import { styled } from '@mui/material';

import { DiffValue } from '../types';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationLabel } from 'tg.component/TranslationLabel';

type LabelModel = components['schemas']['LabelModel'];

const TranslationLabelRemoved = styled(TranslationLabel)`
  text-decoration: line-through;
`;

const StyledAdded = styled('span')`
  color: ${({ theme }) => theme.palette.activity.added};
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
      {removed.map((label, i) => (
        <TranslationLabelRemoved key={label.id} color={label.color}>
          {label.name}
        </TranslationLabelRemoved>
      ))}
      {added.map((label, i) => (
        <>
          <StyledAdded>+</StyledAdded>
          <TranslationLabel key={label.id} color={label.color}>
            {label.name}
          </TranslationLabel>
        </>
      ))}
    </>
  );
};
