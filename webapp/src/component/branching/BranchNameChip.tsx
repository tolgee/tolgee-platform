import { ArrowDropDown, Branch } from 'tg.component/CustomIcons';
import { DefaultChip } from 'tg.component/common/chips/DefaultChip';
import React from 'react';
import { Box } from '@mui/material';

type Props = {
  name: string;
  as?: typeof DefaultChip;
  disabled?: boolean;
  arrow?: boolean;
};

export const BranchNameChip = ({
  name,
  as: ChipComponent = DefaultChip,
  disabled,
  arrow,
}: Props) => {
  return (
    <ChipComponent
      disabled={disabled}
      label={
        <Box display="flex" alignItems="center" flexWrap="nowrap">
          <span>{name}</span>
          {arrow && (
            <ArrowDropDown width={24} height={24} style={{ marginRight: -4 }} />
          )}
        </Box>
      }
      icon={<Branch width={18} height={18} />}
    />
  );
};

export const BranchNameChipNode = ({ children }: { children?: string }) => (
  <BranchNameChip name={children!} />
);
