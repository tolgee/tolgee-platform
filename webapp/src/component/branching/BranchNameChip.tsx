import { ArrowDropDown, Branch } from 'tg.component/CustomIcons';
import { DefaultChip } from 'tg.component/common/chips/DefaultChip';
import React from 'react';
import { Box } from '@mui/material';

type Props = {
  name: string;
  as?: typeof DefaultChip;
  arrow?: boolean;
};

export const BranchNameChip = ({
  name,
  as: ChipComponent = DefaultChip,
  arrow,
}: Props) => {
  return (
    <ChipComponent
      label={
        <Box display="flex" alignItems="center" flexWrap="nowrap">
          <span>{name}</span>
          {arrow && (
            <ArrowDropDown width={24} height={24} style={{ marginRight: -4 }} />
          )}
        </Box>
      }
      icon={<Branch width={20} height={20} />}
    />
  );
};

export const BranchNameChipNode = ({ children }: { children?: string }) => (
  <BranchNameChip name={children!} />
);
