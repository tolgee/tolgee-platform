import { FC, ReactElement } from 'react';
import {
  Box,
  Button,
  Chip,
  styled,
  Theme,
  Tooltip,
  tooltipClasses,
  TooltipProps,
  useTheme,
} from '@mui/material';
import { T } from '@tolgee/react';
import { useTrialInfo } from 'tg.component/layout/TopBar/announcements/useTrialInfo';
import { PlanFeaturesBox, PlanTitle } from '../Plan/PlanStyles';
import { getHighlightColor } from '../Plan/Plan';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { IncludedFeatures } from '../Plan/IncludedFeatures';
import { IncludedUsage } from '../Plan/IncludedUsage';
import { components } from 'tg.service/billingApiSchema.generated';
import { Link } from 'react-router-dom';

const CustomWidthTooltip = styled(({ className, ...props }: TooltipProps) => (
  <Tooltip {...props} classes={{ popper: className }} />
))(({ theme }) => ({
  [`& .${tooltipClasses.tooltip}`]: {
    maxWidth: 420,
    border: `1px solid ${theme.palette.tokens.border.soft}`,
    boxShadow: `0px 0px 20px 0px ${theme.palette.tokens.elevation.pricing}`,
  },
}));

type TrialChipTooltipProps = {
  children: ReactElement;
  onOpen: () => void;
  open: boolean;
  onClose: () => void;
};

export const TrialChipTooltip: FC<TrialChipTooltipProps> = ({
  children,
  onOpen,
  open,
  onClose,
}) => {
  const { preferredOrganization: organization } = usePreferredOrganization();

  const activeSubscription = organization?.activeCloudSubscription;

  const { subscriptionsLink } = useTrialInfo();

  const theme = useTheme();

  const plan = activeSubscription?.plan;

  if (!plan) {
    return null;
  }

  const highlightColor = getHighlightColor(theme, !plan.public);

  const importantFeatures: components['schemas']['CloudPlanModel']['enabledFeatures'] =
    [
      'GRANULAR_PERMISSIONS',
      'AI_PROMPT_CUSTOMIZATION',
      'TASKS',
      'ORDER_TRANSLATION',
      'WEBHOOKS',
      'PROJECT_LEVEL_CONTENT_STORAGES',
      'MULTIPLE_CONTENT_DELIVERY_CONFIGS',
      'SLACK_INTEGRATION',
      'SSO',
      'WEBHOOKS',
    ];

  const filteredFeatures = importantFeatures.filter((f) =>
    plan.enabledFeatures.includes(f)
  );

  return (
    <CustomWidthTooltip
      placement={'bottom-start'}
      onOpen={onOpen}
      onClose={onClose}
      open={open}
      disableTouchListener
      title={
        <>
          <PlanContent data-cy="topbap-trial-popover-content">
            <PlanTitle
              sx={{
                pb: '10px',
                color: highlightColor,
                display: 'flex',
                alignItems: 'center',
              }}
            >
              {plan.name}{' '}
              <TrialChip
                data-cy={`topbar-trial-popover-trial-plan-chip`}
                size="small"
                label={
                  <T keyName="topbar-trial-popover-trial-plan-title-trial-chip" />
                }
              />
            </PlanTitle>

            <Box
              display="flex"
              flexDirection="column"
              alignItems="stretch"
              flexGrow={1}
              sx={{ pt: '10px' }}
              data-cy={`topbar-trial-popover-plan-features`}
            >
              <PlanFeaturesBox sx={{ gap: '18px' }}>
                <IncludedFeatures
                  features={filteredFeatures}
                  sx={(theme: Theme) => ({
                    color: theme.palette.tokens.text.primary,
                  })}
                />
                <IncludedUsage
                  metricType={plan.metricType}
                  includedUsage={plan.includedUsage}
                  highlightColor={highlightColor}
                  sx={(theme: Theme) => ({
                    alignSelf: 'center',
                    color: theme.palette.tokens.text.primary,
                  })}
                />
              </PlanFeaturesBox>
            </Box>
            <Box sx={{ mt: '32px', alignSelf: 'center' }}>
              <Button
                data-cy="topbar-trial-popover-compare-plans-button"
                variant="contained"
                color={!plan.public ? 'info' : 'primary'}
                size="medium"
                component={Link}
                to={subscriptionsLink}
              >
                <T keyName="topbar-trial-popover-compare-plans-button" />
              </Button>
            </Box>
          </PlanContent>
        </>
      }
    >
      {children}
    </CustomWidthTooltip>
  );
};

const TrialChip = styled(Chip)`
  margin-left: 8px;
  background-color: ${({ theme }) => theme.palette.tokens.primary.main};
  color: ${({ theme }) => theme.palette.tokens.primary.contrast};
  text-transform: uppercase;
`;

const PlanContent = styled('div')`
  padding: 40px 40px;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  height: 100%;
`;
