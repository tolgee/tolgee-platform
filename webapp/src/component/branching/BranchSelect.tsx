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
  branch?: BranchModel | number | null;
  onSelect?: (branch: BranchModel) => void;
  onDefaultValue?: (branch: BranchModel) => void;
  hideDefault?: boolean;
  disabled?: boolean;
};

export const BranchSelect = ({
  branch,
  onSelect,
  onDefaultValue,
  hideDefault,
  disabled,
}: Props) => {
  const project = useProject();
  const {
    default: defaultBranch,
    branches,
    loadable,
  } = useBranchesService({
    projectId: project.id,
  });

  const items = branches.filter((b) => !b.isDefault || !hideDefault);

  const defaultValue = !hideDefault
    ? branch
      ? items.find((b) =>
          typeof branch === 'number' ? b.id === branch : b.id === branch.id
        )
      : defaultBranch
    : items[0] || undefined;

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
    onSelect?.(item);
    setSelected(item);
  }

  return (
    <InfiniteSearchSelect
      items={items}
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
      disabled={disabled}
    />
  );
};
