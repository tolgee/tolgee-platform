import { InfiniteSearchSelect } from 'tg.component/searchSelect/InfiniteSearchSelect';
import { BranchLabel } from 'tg.component/branching/BranchLabel';
import React, { useEffect, useState } from 'react';
import { SelectItem } from 'tg.component/searchSelect/SelectItem';
import { DefaultBranchChip } from 'tg.component/branching/DefaultBranchChip';
import { components } from 'tg.service/apiSchema.generated';
import { styled } from '@mui/material';
import { useBranchesService } from 'tg.views/projects/translations/context/services/useBranchesService';
import { useProject } from 'tg.hooks/useProject';

type BranchModel = components['schemas']['BranchModel'];

const StyledLabel = styled('div')`
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

type Props = {
  branchId?: number;
  onSelect: (branch: BranchModel) => void;
  onDefaultValue?: (branch: BranchModel) => void;
};

export const BranchSelect = ({ branchId, onSelect, onDefaultValue }: Props) => {
  const project = useProject();
  const {
    default: defaultBranch,
    branches,
    loadable,
  } = useBranchesService({
    projectId: project.id,
  });

  const defaultValue = branchId
    ? branches.find((b) => b.id === branchId)
    : defaultBranch;

  const [selected, setSelected] = useState<BranchModel | undefined>(
    defaultValue || undefined
  );

  useEffect(() => {
    if (defaultValue) {
      setSelected(defaultValue);
      onDefaultValue?.(defaultValue);
    }
  }, [defaultBranch]);

  function renderItem(props: any, item: BranchModel) {
    return (
      <SelectItem
        {...props}
        label={
          <StyledLabel>
            <div>{item.name}</div>
            {item.isDefault && <DefaultBranchChip />}
          </StyledLabel>
        }
        selected={item.id === selected?.id}
        onClick={() => select(item)}
      />
    );
  }

  function select(item: BranchModel) {
    onSelect(item);
    setSelected(item);
  }

  return (
    <InfiniteSearchSelect
      items={branches}
      queryResult={loadable}
      itemKey={(item) => item.id}
      selected={selected}
      minHeight={false}
      search={''}
      renderItem={renderItem}
      labelItem={(item: BranchModel) => {
        return item?.name;
      }}
      inputComponent={BranchLabel}
      menuAnchorOrigin={{
        vertical: 'bottom',
        horizontal: 'left',
      }}
    />
  );
};
