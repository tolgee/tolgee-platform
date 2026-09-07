import { Box, styled } from '@mui/material';

const pricingShadow = (color: string) => `0px 0px 20px 0px ${color}`;

export const PlanContainer = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  position: relative;
  border: 1px solid ${({ theme }) => theme.palette.background.paper};
  background: ${({ theme }) => theme.palette.background.paper};
  border-radius: 20px;
  overflow: hidden;
  box-shadow: ${({ theme }) =>
    pricingShadow(theme.palette.tokens.elevation.pricing)};
  background: ${({ theme }) => theme.palette.tokens.background['paper-2']};
  border-color: ${({ theme }) => theme.palette.tokens.border.soft};
  &.active {
    box-shadow: ${({ theme }) =>
      pricingShadow(theme.palette.tokens.elevation.pricingActive)};
    border-color: ${({ theme }) =>
      theme.palette.tokens.secondary._states.outlinedBorder};
  }
  &.popular {
    border-color: ${({ theme }) => theme.palette.tokens.primary.main};
    background-image: linear-gradient(
      ${({ theme }) => theme.palette.tokens.primary._states.hover},
      ${({ theme }) => theme.palette.tokens.primary._states.hover}
    );
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
  }
  &.inRow.active {
    border-color: ${({ theme }) =>
      theme.palette.tokens.secondary._states.outlinedBorder};
    box-shadow: ${({ theme }) =>
      pricingShadow(theme.palette.tokens.elevation.pricingActive)};
  }
  &.inRow.popular {
    border-color: ${({ theme }) => theme.palette.tokens.primary.main};
    box-shadow: ${({ theme }) =>
      pricingShadow(theme.palette.tokens.elevation.pricing)};
  }
  ${({ theme }) => theme.breakpoints.down('md')} {
    &.inRow {
      border: 1px solid ${({ theme }) => theme.palette.tokens.border.soft};
      border-radius: 20px;
      box-shadow: ${({ theme }) =>
        pricingShadow(theme.palette.tokens.elevation.pricing)};
    }
    &.inRow.active,
    &.inRow.popular {
      margin: 0;
    }
    &.inRow.active {
      border-color: ${({ theme }) =>
        theme.palette.tokens.secondary._states.outlinedBorder};
      box-shadow: ${({ theme }) =>
        pricingShadow(theme.palette.tokens.elevation.pricingActive)};
    }
    &.inRow.popular {
      border-color: ${({ theme }) => theme.palette.tokens.primary.main};
    }
  }
`;

export const PLAN_ROW_MAX_COLUMNS = 4;

const PLAN_COLUMN_WIDTH_PX = 350;

const PLAN_CONTENT_PADDING_Y_PX = 32;
const PLAN_HEADER_MIN_HEIGHT_PX = 140;
const PLAN_HEADER_STANDOUT_PADDING_TOP_PX = 44;
const PLAN_HEADER_BROKEN_OUT_MIN_HEIGHT_PX = 155;
const PLAN_HEADER_BROKEN_OUT_PADDING_TOP_PX = 39;

export const PlanRow = styled('div')`
  display: grid;
  grid-template-columns: repeat(
    auto-fit,
    minmax(calc(100% / ${PLAN_ROW_MAX_COLUMNS} - 1%), 1fr)
  );
  align-items: stretch;
  max-width: calc(
    var(--plan-columns, ${PLAN_ROW_MAX_COLUMNS}) * ${PLAN_COLUMN_WIDTH_PX}px
  );
  margin-inline: auto;
  border: 1px solid ${({ theme }) => theme.palette.tokens.border.soft};
  border-radius: 20px;
  background: ${({ theme }) => theme.palette.tokens.background['paper-2']};
  box-shadow: ${({ theme }) =>
    pricingShadow(theme.palette.tokens.elevation.pricing)};
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
  &.unjoined {
    border: none;
    background: none;
    box-shadow: none;
    gap: ${({ theme }) => theme.spacing(2)};
    & > * + *::before {
      content: none;
    }
  }
  ${({ theme }) => theme.breakpoints.down('md')} {
    grid-auto-flow: row;
    grid-template-columns: repeat(2, 1fr);
    border: none;
    background: none;
    box-shadow: none;
    gap: ${({ theme }) => theme.spacing(2)};
    & > * + *::before {
      content: none;
    }
  }
`;

export const PlanContent = styled('div')`
  padding: ${PLAN_CONTENT_PADDING_Y_PX}px 24px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  height: 100%;
  .inRow.active > &,
  .inRow.popular > & {
    padding-bottom: 47px;
  }
  ${({ theme }) => theme.breakpoints.down('md')} {
    .inRow.active > &,
    .inRow.popular > & {
      padding-bottom: ${PLAN_CONTENT_PADDING_Y_PX}px;
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
  min-height: ${PLAN_HEADER_MIN_HEIGHT_PX}px;
  background: ${({ theme }) => theme.palette.tokens.background['paper-3']};
  &.active {
    background: ${({ theme }) => theme.palette.tokens.secondary.main};
    color: ${({ theme }) => theme.palette.tokens.secondary.contrast};
  }
  &.popular {
    background: ${({ theme }) => theme.palette.tokens.primary.main};
    color: ${({ theme }) => theme.palette.tokens.primary.contrast};
  }
  &.active,
  &.popular {
    padding-top: ${PLAN_HEADER_STANDOUT_PADDING_TOP_PX}px;
  }
  .inRow.active > &,
  .inRow.popular > & {
    min-height: ${PLAN_HEADER_BROKEN_OUT_MIN_HEIGHT_PX}px;
    padding-top: ${PLAN_HEADER_BROKEN_OUT_PADDING_TOP_PX}px;
  }
  ${({ theme }) => theme.breakpoints.down('md')} {
    .inRow.active > &,
    .inRow.popular > & {
      min-height: ${PLAN_HEADER_MIN_HEIGHT_PX}px;
      padding-top: ${PLAN_HEADER_STANDOUT_PADDING_TOP_PX}px;
    }
  }
`;

// Must stay fully inside PlanHeader, its offset parent: PlanContainer sets `overflow: hidden` to
// clip the header band to the card's radius, so a pill straddling that edge is cut in half.
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
