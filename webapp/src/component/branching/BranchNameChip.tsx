import { ArrowDropDown, Branch } from 'tg.component/CustomIcons';
import { DefaultChip } from 'tg.component/common/chips/DefaultChip';
import React from 'react';
import { Box, styled } from '@mui/material';
import { ShieldTick } from '@untitled-ui/icons-react';

type Props = {
  name: string;
  as?: typeof DefaultChip;
  disabled?: boolean;
  arrow?: boolean;
  size?: 'small' | 'default';
  isProtected?: boolean;
};

const StyledShieldTick = styled(ShieldTick)`
  margin-left: 4px;
`;

export const BranchNameChip = ({
  name,
  as: ChipComponent = DefaultChip,
  disabled,
  arrow,
  size = 'default',
  isProtected,
}: Props) => {
  const iconSize = size === 'small' ? 14 : 18;
  const dropdownIconSize = size === 'small' ? 18 : 24;
  const protectedIconSize = size === 'small' ? 14 : 18;
  return (
    <ChipComponent
      disabled={disabled}
      size={size === 'small' ? 'small' : undefined}
      label={
        <Box display="flex" alignItems="center" flexWrap="nowrap">
          <span>{name}</span>
          {isProtected && (
            <StyledShieldTick
              width={protectedIconSize}
              height={protectedIconSize}
            />
          )}
          {arrow && (
            <ArrowDropDown
              width={dropdownIconSize}
              height={dropdownIconSize}
              style={{ marginRight: -4 }}
            />
          )}
        </Box>
      }
      icon={<Branch width={iconSize} height={iconSize} />}
    />
  );
};

export const BranchNameChipNode = ({
  children,
  size,
}: {
  children?: string;
  size?: 'small' | 'default';
}) => <BranchNameChip name={children!} size={size} />;
