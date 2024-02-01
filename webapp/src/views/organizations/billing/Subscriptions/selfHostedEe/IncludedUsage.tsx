import {Box} from "@mui/material";
import {T} from "@tolgee/react";
import {components} from "tg.service/apiSchema.generated";

interface IncludedProps {
  included: components['schemas']['PlanIncludedUsageModel'];
}

export function IncludedUsage(props: IncludedProps) {
  return (
    <>
      {props.included.seats > 0 && (
        <Box>
          <T
            keyName="billinb_self_hosted_plan_included_seats"
            params={{seats: props.included.seats}}
          />
        </Box>
      )}
      {props.included.seats == -1 && (
        <Box>
          <T keyName="billinb_self_hosted_plan_unlimited_seats"/>
        </Box>
      )}
      {props.included.mtCredits > 0 && (
        <Box>
          <T
            keyName="billinb_self_hosted_plan_included_mtCredits"
            params={{mtCredits: props.included.mtCredits}}
          />
        </Box>
      )}
    </>
  );
}
