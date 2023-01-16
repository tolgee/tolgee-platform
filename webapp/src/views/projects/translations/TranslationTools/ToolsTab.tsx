import React from 'react';
import { styled, Typography, Link as MuiLink } from '@mui/material';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';

import { TabMessage } from './TabMessage';
import { UseQueryResult } from 'react-query';
import { useConfig, usePreferredOrganization } from 'tg.globalContext/helpers';
import { LINKS, PARAMS } from 'tg.constants/links';
import { MtHint } from 'tg.component/billing/MtHint';

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
  min-width: 0px;
`;

const StyledTab = styled('div')`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  padding: ${({ theme }) => theme.spacing(0.5, 1)};
  background: ${({ theme }) => theme.palette.cellSelected1.main};
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1.main};
  text-transform: uppercase;
  color: ${({ theme }) => theme.palette.text.secondary};
  position: sticky;
  top: 0px;
  height: 32px;
  flex-shrink: 1;
  flex-basis: 0px;
`;

const StyledBadge = styled('div')`
  background: ${({ theme }) => theme.palette.emphasis[200]};
  padding: 2px 4px;
  border-radius: 12px;
  font-size: 12px;
  height: 20px;
  min-width: 20px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
`;

const StyledTitle = styled(Typography)`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
`;

type Props = {
  icon: React.ReactNode;
  title: string;
  badgeNumber?: number;
  data?: UseQueryResult<unknown, any>;
};

export const ToolsTab: React.FC<Props> = ({
  icon,
  title,
  badgeNumber,
  children,
  data,
}) => {
  const { preferredOrganization } = usePreferredOrganization();
  const config = useConfig();

  const getCreditHint = (code: string) => {
    if (code === 'out_of_credits') {
      if (
        preferredOrganization.currentUserRole === 'OWNER' &&
        config.billing.enabled
      ) {
        return (
          <T
            keyName="translation_tools_no_credits_billing_link"
            params={{
              link: (
                <MuiLink
                  component={Link}
                  to={LINKS.ORGANIZATION_BILLING.build({
                    [PARAMS.ORGANIZATION_SLUG]: preferredOrganization.slug,
                  })}
                />
              ),
              hint: <MtHint />,
            }}
          />
        );
      } else {
        return (
          <T
            keyName="translation_tools_no_credits_message"
            params={{
              hint: <MtHint />,
            }}
          />
        );
      }
    }
  };

  const error = data?.error;
  const errorCode = error?.message || error?.code || error || 'Unknown error';

  const creditHint = getCreditHint(errorCode);

  return (
    <StyledContainer>
      <StyledTab>
        {icon}
        <StyledTitle variant="button">{title}</StyledTitle>
        {badgeNumber ? <StyledBadge>{badgeNumber}</StyledBadge> : null}
      </StyledTab>

      {data?.isError ? (
        <TabMessage
          type={creditHint ? 'placeholder' : 'error'}
          message={creditHint || errorCode}
        />
      ) : (
        children
      )}
    </StyledContainer>
  );
};
