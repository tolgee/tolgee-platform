import { styled, Tooltip, tooltipClasses, TooltipProps } from '@mui/material';

export const FullWidthTooltip = styled(
  ({ className, ...props }: TooltipProps) => (
    <Tooltip {...props} classes={{ popper: className }} />
  )
)({
  [`& .${tooltipClasses.tooltip}`]: {
    maxWidth: 'none',
  },
});
