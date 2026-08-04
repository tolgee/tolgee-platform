import { Chip, styled } from '@mui/material';

const StyledChips = styled('div')`
  display: flex;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.spacing(0.75)};
`;

type AppChipsProps = {
  items: string[];
  dataCy: string;
  color?: 'default' | 'info' | 'warning';
  variant?: 'filled' | 'outlined';
};

export const AppChips = ({ items, dataCy, color, variant }: AppChipsProps) => {
  if (items.length === 0) {
    return null;
  }
  return (
    <StyledChips data-cy={dataCy}>
      {items.map((item) => (
        <Chip
          key={item}
          size="small"
          color={color}
          variant={variant}
          label={item}
        />
      ))}
    </StyledChips>
  );
};
