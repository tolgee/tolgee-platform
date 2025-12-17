import React, { useEffect, useMemo, useState } from 'react';
import { DefaultBranchChip } from 'tg.component/branching/DefaultBranchChip';
import { components } from 'tg.service/apiSchema.generated';
import { Box, Menu, MenuItem, styled } from '@mui/material';
import { useBranchesService } from 'tg.views/projects/translations/context/services/useBranchesService';
import { useProject } from 'tg.hooks/useProject';
import { InfiniteSearchSelectContent } from 'tg.component/searchSelect/InfiniteSearchSelectContent';
import { BranchNameChip } from 'tg.component/branching/BranchNameChip';
import clsx from 'clsx';
import { TransparentChip } from 'tg.component/common/chips/TransparentChip';

type BranchModel = components['schemas']['BranchModel'];

const StyledLabel = styled('div')`
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
`;

const Label = styled('div')`
  cursor: pointer;

  &.disabled {
    cursor: default;
  }
`;

type Props = {
  branch?: BranchModel | number | string | null;
  onSelect?: (branch: BranchModel) => void;
  onDefaultValue?: (branch: BranchModel) => void;
  hideDefault?: boolean;
  disabled?: boolean;
  hiddenIds?: number[];
};

export const BranchSelect = ({
  branch,
  onSelect,
  onDefaultValue,
  hideDefault,
  disabled,
  hiddenIds,
}: Props) => {
  const project = useProject();
  const { default: defaultBranch, branches } = useBranchesService({
    projectId: project.id,
  });

  const items = branches.filter(
    (b) => (!b.isDefault || !hideDefault) && !hiddenIds?.includes(b.id)
  );

  const derivedSelected = useMemo(() => {
    if (!items.length) {
      return undefined;
    }
    if (branch) {
      return items.find((b) =>
        typeof branch === 'number'
          ? b.id === branch
          : typeof branch === 'string'
          ? b.name === branch
          : b.id === branch.id
      );
    }
    return (!hideDefault ? defaultBranch : items[0]) ?? undefined;
  }, [branch, items, defaultBranch, hideDefault]);

  const [selected, setSelected] = useState<BranchModel | undefined>(
    derivedSelected
  );

  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  useEffect(() => {
    if (!derivedSelected) {
      return;
    }

    if (derivedSelected.id !== selected?.id) {
      setSelected(derivedSelected);
    }

    if (!branch) {
      onDefaultValue?.(derivedSelected);
    }
  }, [branch, derivedSelected, onDefaultValue, selected?.id]);

  function renderItem(props: any, item: BranchModel) {
    return (
      <MenuItem
        {...props}
        selected={item.id === selected?.id}
        onClick={() => select(item)}
      >
        <StyledLabel>
          <div>{item.name}</div>
          {item.isDefault && <DefaultBranchChip />}
        </StyledLabel>
      </MenuItem>
    );
  }

  function select(item: BranchModel) {
    onSelect?.(item);
    setSelected(item);
    handleClose();
  }

  const handleOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleKeyUp = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Escape') {
      handleClose();
    }
  };

  return (
    <Box display="flex" alignItems="center">
      <Label
        onClick={!disabled ? handleOpen : undefined}
        className={clsx(disabled && 'disabled')}
      >
        {selected && (
          <BranchNameChip
            name={selected.name}
            as={TransparentChip}
            disabled={disabled}
            arrow
          />
        )}
      </Label>
      <Menu
        open={Boolean(anchorEl)}
        anchorEl={anchorEl}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'left',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'left',
        }}
        slotProps={{
          paper: { style: { marginTop: 8 } },
        }}
        onKeyUp={handleKeyUp}
        onClose={handleClose}
      >
        <InfiniteSearchSelectContent<BranchModel>
          open={Boolean(anchorEl)}
          onClose={handleClose}
          items={items}
          itemKey={(item) => item.id}
          search={''}
          renderOption={renderItem}
        />
      </Menu>
    </Box>
  );
};
