import React from 'react';
import { BranchNameLink } from 'tg.ee.module/branching/components/BranchNameLink';
import { BranchReferenceData } from '../../../eeSetup/EeModuleType';

type Props = {
  data: BranchReferenceData;
};

export const BranchReference: React.FC<Props> = ({ data }) => {
  return (
    <span className="reference referenceText">
      <BranchNameLink name={data.name} />
    </span>
  );
};
