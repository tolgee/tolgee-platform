import { ArrowDropDown, Branch } from 'tg.component/CustomIcons';
import { DefaultChip } from 'tg.component/common/chips/DefaultChip';
import React from 'react';
import { Box } from '@mui/material';

type Props = {
  name: string;
  as?: typeof DefaultChip;
  disabled?: boolean;
  arrow?: boolean;
  size?: 'small' | 'default';
};

export const BranchNameChip = ({
  name,
  as: ChipComponent = DefaultChip,
  disabled,
  arrow,
  size = 'default',
}: Props) => {
  const iconSize = size === 'small' ? 14 : 18;
  const dropdownIconSize = size === 'small' ? 18 : 24;
  return (
    <ChipComponent
      disabled={disabled}
      size={size === 'small' ? 'small' : undefined}
      label={
        <Box display="flex" alignItems="center" flexWrap="nowrap">
          <span>{name}</span>
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
