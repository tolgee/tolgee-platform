import { Box } from '@mui/material';
import { T } from '@tolgee/react';

export type Usage = {
  seats: number;
  mtCredits?: number;
};

interface Props {
  included: Usage;
}

export function IncludedUsage(props: Props) {
  return (
    <>
      {props.included.seats > 0 && (
        <Box>
          <T
            keyName="billinb_self_hosted_plan_included_seats"
            params={{ seats: props.included.seats }}
          />
        </Box>
      )}
      {props.included.seats == -1 && (
        <Box>
          <T keyName="billinb_self_hosted_plan_unlimited_seats" />
        </Box>
      )}
      {props.included.mtCredits !== undefined &&
        props.included.mtCredits > 0 && (
          <Box>
            <T
              keyName="billinb_self_hosted_plan_included_mtCredits"
              params={{ mtCredits: props.included.mtCredits }}
            />
          </Box>
        )}
    </>
  );
}
