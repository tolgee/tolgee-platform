import React, { ReactNode } from 'react';
import { Box, Radio, FormControl, Typography, styled } from '@mui/material';

const StyledRadioOption = styled(Box)<{ selected?: boolean }>`
  border: 2px solid
    ${({ theme, selected }) =>
      selected ? theme.palette.primary.main : theme.palette.divider};
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  padding-top: ${({ theme }) => theme.spacing(1.5)};
  padding-bottom: ${({ theme }) => theme.spacing(2)};
  padding-left: ${({ theme }) => theme.spacing(2)};
  padding-right: ${({ theme }) => theme.spacing(2)};
  flex: 1;
  transition: border-color 0.2s ease;
  cursor: pointer;

  &:hover {
    border-color: ${({ theme, selected }) =>
      selected ? theme.palette.primary.main : theme.palette.primary.light};
  }
`;

export type ModeOption<T> = {
  value: T;
  title: ReactNode;
  description: ReactNode;
  dataCy?: string;
};

export type ModeSelectorProps<T> = {
  value: T;
  onChange: (mode: T) => void;
  options: ModeOption<T>[];
};

export function ModeSelector<T extends React.Key>({
  value,
  onChange,
  options,
}: ModeSelectorProps<T>) {
  return (
    <FormControl component="fieldset" fullWidth>
      <Box display="flex" gap={2} mb={2}>
        {options.map((option) => (
          <StyledRadioOption
            key={option.value}
            selected={value === option.value}
            onClick={() => onChange(option.value)}
          >
            <Box display="flex" flexDirection="column" width="100%">
              <Box
                display="flex"
                justifyContent="space-between"
                alignItems="center"
                mb={0.5}
              >
                <Typography variant="body1" fontWeight={500}>
                  {option.title}
                </Typography>
                <Radio
                  checked={value === option.value}
                  onChange={(event) => {
                    event.stopPropagation();
                    onChange(option.value);
                  }}
                  onClick={(event) => event.stopPropagation()}
                  data-cy={option.dataCy}
                  size="small"
                  sx={{ padding: 0.5 }}
                />
              </Box>
              <Typography variant="body2" color="text.secondary">
                {option.description}
              </Typography>
            </Box>
          </StyledRadioOption>
        ))}
      </Box>
    </FormControl>
  );
}
