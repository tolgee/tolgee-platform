import clsx from 'clsx';
import { alpha, styled } from '@mui/material';

const StyledGroup = styled('div')`
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: 1fr;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-radius: ${({ theme }) => theme.shape.borderRadius * 2}px;
  overflow: hidden;
`;

const StyledSegment = styled('button')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.25)};
  justify-items: center;
  padding: ${({ theme }) => theme.spacing(1.25, 1)};
  background: transparent;
  border: none;
  border-left: 1px solid ${({ theme }) => theme.palette.divider1};
  cursor: pointer;
  font: inherit;
  color: ${({ theme }) => theme.palette.text.primary};

  &:first-of-type {
    border-left: none;
  }

  &.selected {
    background: ${({ theme }) => alpha(theme.palette.primary.main, 0.08)};

    & .title {
      color: ${({ theme }) => theme.palette.primary.main};
    }
  }

  &:disabled {
    cursor: default;
    color: ${({ theme }) => theme.palette.text.disabled};

    & .title,
    & .subtitle {
      color: ${({ theme }) => theme.palette.text.disabled};
    }
  }

  & .title {
    font-weight: ${({ theme }) => theme.typography.fontWeightMedium};
  }

  & .subtitle {
    font-size: ${({ theme }) => theme.typography.caption.fontSize}px;
    color: ${({ theme }) => theme.palette.text.secondary};
  }
`;

export type SegmentedControlOption<V extends string> = {
  value: V;
  title: string;
  subtitle?: string;
  dataCy: string;
};

type Props<V extends string> = {
  options: SegmentedControlOption<V>[];
  value: V;
  disabled?: boolean;
  dataCy: string;
  onChange: (value: V) => void;
};

export function SegmentedControl<V extends string>({
  options,
  value,
  disabled,
  dataCy,
  onChange,
}: Props<V>) {
  return (
    <StyledGroup data-cy={dataCy}>
      {options.map((option) => (
        <StyledSegment
          key={option.value}
          type="button"
          disabled={disabled}
          className={clsx({ selected: option.value === value })}
          data-cy={option.dataCy}
          data-cy-option={option.value}
          data-cy-selected={String(option.value === value)}
          onClick={() => onChange(option.value)}
        >
          <span className="title">{option.title}</span>
          {option.subtitle !== undefined && (
            <span className="subtitle">{option.subtitle}</span>
          )}
        </StyledSegment>
      ))}
    </StyledGroup>
  );
}
