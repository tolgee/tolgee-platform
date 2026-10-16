import { ReactNode } from 'react';
import { Checkbox, FormControlLabel, styled, Typography } from '@mui/material';

const StyledSection = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.5)};
  margin-bottom: ${({ theme }) => theme.spacing(3)};
`;

const StyledGridItems = styled('div')`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(260px, 100%), 1fr));
`;

const StyledListItems = styled('div')`
  display: grid;
  justify-items: start;
`;

const StyledLabel = styled('span')`
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.5, 1)};
`;

export type FeatureCheckboxItem = {
  key: string;
  label: ReactNode;
  chip?: ReactNode;
  checked: boolean;
  disabled: boolean;
  onChange: (checked: boolean) => void;
  checkboxAttributes: Record<`data-cy-${string}`, string>;
};

type Props = {
  title: ReactNode;
  hint: ReactNode;
  layout: 'list' | 'grid';
  checkboxDataCy: string;
  items: FeatureCheckboxItem[];
};

export const FeatureCheckboxSection = ({
  title,
  hint,
  layout,
  checkboxDataCy,
  items,
}: Props) => {
  const Items = layout === 'grid' ? StyledGridItems : StyledListItems;

  return (
    <StyledSection>
      <Typography variant="h6">{title}</Typography>
      <Typography variant="body2" color="text.secondary">
        {hint}
      </Typography>
      <Items>
        {items.map((item) => (
          <FormControlLabel
            key={item.key}
            disabled={item.disabled}
            control={
              <Checkbox
                data-cy={checkboxDataCy}
                {...item.checkboxAttributes}
                checked={item.checked}
                onChange={(_, checked) => item.onChange(checked)}
              />
            }
            label={
              <StyledLabel>
                {item.label}
                {item.chip}
              </StyledLabel>
            }
          />
        ))}
      </Items>
    </StyledSection>
  );
};
