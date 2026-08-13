import { Box, styled } from '@mui/material';

export const PlanContainer = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  position: relative;
  border: 1px solid ${({ theme }) => theme.palette.background.paper};
  background: ${({ theme }) => theme.palette.background.paper};
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0px 0px 20px 0px
    ${({ theme }) => theme.palette.tokens.elevation.pricing};
  background: ${({ theme }) => theme.palette.tokens.background['paper-2']};
  border-color: ${({ theme }) => theme.palette.tokens.border.soft};
  &.active {
    box-shadow: 0px 0px 20px 0px
      ${({ theme }) => theme.palette.tokens.elevation.pricingActive};
    border-color: ${({ theme }) =>
      theme.palette.tokens.secondary._states.outlinedBorder};
  }
  &.inRow {
    border: none;
    border-radius: 0;
    box-shadow: none;
  }
  &.inRow.active {
    border: 1px solid
      ${({ theme }) => theme.palette.tokens.secondary._states.outlinedBorder};
    border-radius: 20px;
    margin: -16px 0;
    box-shadow: 0px 0px 20px 0px
      ${({ theme }) => theme.palette.tokens.elevation.pricingActive};
  }
  ${({ theme }) => theme.breakpoints.down('md')} {
    /* The row stops being a joined surface here and becomes a grid of separate cards,
       so every card gets its own chrome back — not just the active one. */
    &.inRow {
      border: 1px solid ${({ theme }) => theme.palette.tokens.border.soft};
      border-radius: 20px;
      box-shadow: 0px 0px 20px 0px
        ${({ theme }) => theme.palette.tokens.elevation.pricing};
    }
    &.inRow.active {
      margin: 0;
      border-color: ${({ theme }) =>
        theme.palette.tokens.secondary._states.outlinedBorder};
      box-shadow: 0px 0px 20px 0px
        ${({ theme }) => theme.palette.tokens.elevation.pricingActive};
    }
  }
`;

export const PlanRow = styled('div')`
  display: grid;
  /* A 24% floor caps the row at four cards — a fifth cannot fit and wraps instead of
     squeezing every column narrower. auto-fit collapses the unused tracks, so three
     cards still stretch across the full width. */
  grid-template-columns: repeat(auto-fit, minmax(24%, 1fr));
  align-items: stretch;
  border: 1px solid ${({ theme }) => theme.palette.tokens.border.soft};
  border-radius: 20px;
  background: ${({ theme }) => theme.palette.tokens.background['paper-2']};
  /* The divider is a pseudo-element, not a border: PlanContainer's own .inRow rule
     sets border:none at higher specificity and would silently win. */
  & > * + *::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    border-left: 1px solid ${({ theme }) => theme.palette.tokens.border.soft};
  }
  ${({ theme }) => theme.breakpoints.down('md')} {
    grid-auto-flow: row;
    grid-template-columns: repeat(2, 1fr);
    border: none;
    background: none;
    gap: 16px;
    & > * + *::before {
      content: none;
    }
  }
`;

export const PlanContent = styled('div')`
  padding: 32px 24px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  height: 100%;
`;

export const PlanTitle = styled('div')`
  align-self: center;
  font-size: 24px;
`;

export const PlanSubtitle = styled('div')`
  position: absolute;
  gap: 4px;
  top: 0px;
  left: 0px;
  right: 0px;
  font-size: 18px;
  text-align: center;
  font-weight: 500;
  color: ${({ theme }) => theme.palette.tokens.secondary.main};
  background: ${({ theme }) => theme.palette.tokens.secondary._states.selected};
`;

export const PlanFeaturesBox = styled(Box)`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  border-radius: 20px;
  padding: 24px 20px;
  background: ${({ theme }) => theme.palette.tokens.background['paper-3']};
`;

export const PlanHeader = styled('div')`
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 24px;
  /* A contact-us plan has one line where the others have price + period; without a
     floor its band is shorter and the row's header stops reading as continuous. */
  min-height: 140px;
  background: ${({ theme }) => theme.palette.tokens.background['paper-3']};
  &.highlighted {
    background: ${({ theme }) => theme.palette.tokens.secondary.main};
    color: ${({ theme }) => theme.palette.tokens.secondary.contrast};
  }
`;

// Sits on PlanHeader's top edge (see PlanActiveBanner's `pill` mode) — needs
// PlanHeader's `position: relative` as its offset parent.
export const PlanHeaderBadge = styled('div')`
  position: absolute;
  top: 0px;
  left: 50%;
  transform: translate(-50%, -50%);
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  color: ${({ theme }) => theme.palette.tokens.secondary.main};
  background: ${({ theme }) => theme.palette.tokens.background['paper-1']};
`;
