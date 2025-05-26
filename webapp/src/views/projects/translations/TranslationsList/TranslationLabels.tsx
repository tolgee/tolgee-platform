import { styled } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationLabel } from 'tg.component/TranslationLabel';

type LabelModel = components['schemas']['LabelModel'];

const StyledLabels = styled('div')`
  display: flex;
  grid-area: labels;
  gap: 8px;
  align-items: center;
  font-size: 13px;
`;

type Props = {
  labels: LabelModel[] | undefined;
  className: string;
};

export const TranslationLabels = ({ labels, className }: Props) => {
  return (
    <StyledLabels className={className}>
      {labels &&
        labels.map((label) => (
          <TranslationLabel
            color={label.color}
            key={label.id}
            tooltip={label.description}
          >
            {label.name}
          </TranslationLabel>
        ))}
    </StyledLabels>
  );
};
