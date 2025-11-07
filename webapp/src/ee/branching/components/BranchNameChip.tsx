import { Branch } from 'tg.component/CustomIcons';
import { DefaultChip } from 'tg.component/common/chips/DefaultChip';
import React from 'react';

type Props = {
  name: string;
};

export const BranchNameChip = ({ name }: Props) => {
  return <DefaultChip label={name} icon={<Branch width={20} height={20} />} />;
};

export const BranchNameChipNode = ({ children }: { children?: string }) => (
  <BranchNameChip name={children!} />
);
