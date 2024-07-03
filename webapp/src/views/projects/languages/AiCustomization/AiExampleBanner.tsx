import { Box, styled } from '@mui/material';
import { Stars } from 'tg.component/CustomIcons';

const StyledWrapper = styled(Box)`
  border-radius: 4px;
  border: 1px solid ${({ theme }) => theme.palette.exampleBanner.border};
  background: ${({ theme }) => theme.palette.exampleBanner.background};
  padding: 16px 20px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
`;

const StyledLabel = styled(Box)`
  display: flex;
  gap: 8px;
  color: ${({ theme }) => theme.palette.primary.main};
`;

const StyledItems = styled(Box)`
  display: grid;
  color: ${({ theme }) => theme.palette.exampleBanner.text};
`;

type Props = {
  label: string;
  items: string[];
  action: React.ReactNode;
};

export const AiExampleBanner = ({ label, items, action }: Props) => {
  return (
    <StyledWrapper>
      <Box display="flex" gap={1} py="8px">
        <StyledLabel>
          <Stars />
          <div>{label}</div>
        </StyledLabel>
        <StyledItems>
          {items.map((item, i) => (
            <Box key={i}>{item}</Box>
          ))}
        </StyledItems>
      </Box>
      <Box>{action}</Box>
    </StyledWrapper>
  );
};
