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
  &.active,
  &.popular {
    box-shadow: 0px 0px 20px 0px
      ${({ theme }) => theme.palette.tokens.elevation.pricingActive};
  }
  &.active {
    border-color: ${({ theme }) =>
      theme.palette.tokens.secondary._states.outlinedBorder};
  }
  &.popular {
    border-color: ${({ theme }) =>
      theme.palette.tokens.primary._states.outlinedBorder};
  }
  &.inRow {
    border: none;
    border-radius: 0;
    box-shadow: none;
  }
  &.inRow.active,
  &.inRow.popular {
    border-style: solid;
    border-width: 1px;
    border-radius: 20px;
    margin: -16px 0;
    box-shadow: 0px 0px 20px 0px
      ${({ theme }) => theme.palette.tokens.elevation.pricingActive};
  }
  &.inRow.active {
    border-color: ${({ theme }) =>
      theme.palette.tokens.secondary._states.outlinedBorder};
  }
  &.inRow.popular {
    border-color: ${({ theme }) =>
      theme.palette.tokens.primary._states.outlinedBorder};
  }
  ${({ theme }) => theme.breakpoints.down('md')} {
    /* The row stops being a joined surface here and becomes a grid of separate cards,
       so every card gets its own chrome back — not just the standout one. */
    &.inRow {
      border: 1px solid ${({ theme }) => theme.palette.tokens.border.soft};
      border-radius: 20px;
      box-shadow: 0px 0px 20px 0px
        ${({ theme }) => theme.palette.tokens.elevation.pricing};
    }
    &.inRow.active,
    &.inRow.popular {
      margin: 0;
      box-shadow: 0px 0px 20px 0px
        ${({ theme }) => theme.palette.tokens.elevation.pricingActive};
    }
    &.inRow.active {
      border-color: ${({ theme }) =>
        theme.palette.tokens.secondary._states.outlinedBorder};
    }
    &.inRow.popular {
      border-color: ${({ theme }) =>
        theme.palette.tokens.primary._states.outlinedBorder};
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
  /* The other half of the standout card's break-out: it extends 16px past the row's
     bottom edge, and the usage and CTA are bottom-aligned, so without this they ride
     16px lower than the plain cards'. Less the 1px border only a standout card draws. */
  .inRow.active > &,
  .inRow.popular > & {
    padding-bottom: 47px;
  }
  ${({ theme }) => theme.breakpoints.down('md')} {
    .inRow.active > &,
    .inRow.popular > & {
      padding-bottom: 32px;
    }
  }
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
  &.active {
    background: ${({ theme }) => theme.palette.tokens.secondary.main};
    color: ${({ theme }) => theme.palette.tokens.secondary.contrast};
  }
  &.popular {
    background: ${({ theme }) => theme.palette.tokens.primary.main};
    color: ${({ theme }) => theme.palette.tokens.primary.contrast};
  }
  /* Room for PlanHeaderBadge, which is out of flow and would otherwise sit on top
     of the plan name. */
  &.active,
  &.popular {
    padding-top: 44px;
  }
  /* A standout card is pulled 16px out of the row's top edge, which would carry its
     band and everything below it up with it. The band absorbs those 16px (less the
     1px border only a standout card draws) so its bottom edge, the plan name and the
     price stay level with the plain cards. */
  .inRow.active > &,
  .inRow.popular > & {
    min-height: 155px;
    padding-top: 39px;
  }
  ${({ theme }) => theme.breakpoints.down('md')} {
    .inRow.active > &,
    .inRow.popular > & {
      min-height: 140px;
      padding-top: 44px;
    }
  }
`;

// Sits near the top of PlanHeader, inside the band (see PlanActiveBanner's `pill`
// mode) — needs PlanHeader's `position: relative` as its offset parent. It must
// stay fully inside: PlanContainer sets `overflow: hidden` to clip the header band
// to the card's radius, so a pill straddling that edge is cut in half. The header
// reserves room for it with its own top padding.
export const PlanHeaderBadge = styled('div')`
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
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
