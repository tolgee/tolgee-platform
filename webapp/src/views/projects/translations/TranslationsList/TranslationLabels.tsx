import { styled } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationLabel } from 'tg.component/TranslationLabel';
import { LabelControl } from 'tg.views/projects/translations/TranslationsList/Label/LabelControl';

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
  onSelect?: (labelId: number) => void;
};

export const TranslationLabels = ({ labels, className, onSelect }: Props) => {
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
      <LabelControl className="clickable" onSelect={onSelect} />
    </StyledLabels>
  );
};
