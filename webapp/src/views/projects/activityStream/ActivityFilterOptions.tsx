import React from 'react';
import {
  Box,
  Checkbox,
  Divider,
  IconButton,
  ListItemText,
  MenuItem,
} from '@mui/material';
import { ChevronDown } from '@untitled-ui/icons-react';
import { LabelHint } from 'tg.component/common/LabelHint';

export type MultiSelectOption = { value: string; label?: string };
export type MultiSelectPreset = { label: string; values: string[] };

const sameSet = (a: string[], b: string[]) =>
  a.length === b.length && a.every((v) => b.includes(v));

type Props = {
  value: string[];
  onChange: (value: string[]) => void;
  options: MultiSelectOption[];
  presets: MultiSelectPreset[];
};

export const activityFilterOptions = ({
  value,
  onChange,
  options,
  presets,
}: Props) => {
  const all = options.map((o) => o.value);
  const toggle = (v: string) =>
    onChange(value.includes(v) ? value.filter((i) => i !== v) : [...value, v]);

  return [
    ...presets.map((preset, i) => {
      const checked = sameSet(preset.values, value);
      const isAll = sameSet(preset.values, all);
      return (
        <MenuItem
          key={`preset-${i}`}
          value=""
          onClick={() => onChange(checked ? [] : preset.values)}
        >
          <Checkbox
            checked={checked}
            indeterminate={isAll && !checked && value.length > 0}
            size="small"
          />
          <ListItemText primary={preset.label} />
        </MenuItem>
      );
    }),
    <Divider key="divider" />,
    ...options.map((o) => (
      <MenuItem key={o.value} value={o.value} onClick={() => toggle(o.value)}>
        <Checkbox checked={value.includes(o.value)} size="small" />
        <ListItemText primary={o.label} />
      </MenuItem>
    )),
  ];
};

const LabelWithHint = ({
  label,
  hint,
  small,
}: {
  label: string;
  hint?: string;
  small?: boolean;
}) => (
  <ListItemText
    primary={hint ? <LabelHint title={hint}>{label}</LabelHint> : label}
    primaryTypographyProps={small ? { variant: 'body2' } : undefined}
  />
);

export type TreeGroup = {
  value: string;
  label: string;
  children: { value: string; label: string; hint?: string; types: string[] }[];
};

type TreeProps = {
  value: string[];
  onChange: (value: string[]) => void;
  presets: MultiSelectPreset[];
  groups: TreeGroup[];
  expanded: string[];
  onToggleExpanded: (group: string) => void;
};

export const activityTreeOptions = ({
  value,
  onChange,
  presets,
  groups,
  expanded,
  onToggleExpanded,
}: TreeProps) => {
  const all = groups.flatMap((g) => g.children.flatMap((c) => c.types));

  const items: React.ReactElement[] = presets.map((preset, i) => {
    const checked = sameSet(preset.values, value);
    const isAll = sameSet(preset.values, all);
    return (
      <MenuItem
        key={`preset-${i}`}
        value=""
        onClick={() => onChange(checked ? [] : preset.values)}
      >
        <Checkbox
          checked={checked}
          indeterminate={isAll && !checked && value.length > 0}
          size="small"
        />
        <ListItemText primary={preset.label} />
      </MenuItem>
    );
  });
  items.push(<Divider key="divider" />);

  const setTypes = (types: string[], on: boolean) =>
    onChange(
      on
        ? [...value.filter((v) => !types.includes(v)), ...types]
        : value.filter((v) => !types.includes(v))
    );
  const countSelected = (types: string[]) =>
    types.filter((t) => value.includes(t)).length;

  groups.forEach((group) => {
    const types = group.children.flatMap((c) => c.types);
    const selectedCount = countSelected(types);
    const full = selectedCount === types.length;
    const expandable = group.children.length > 1;
    const isOpen = expandable && expanded.includes(group.value);
    items.push(
      <MenuItem
        key={`group-${group.value}`}
        value=""
        sx={{ pr: 1 }}
        onClick={() => setTypes(types, !full)}
      >
        <Checkbox
          checked={full}
          indeterminate={!full && selectedCount > 0}
          size="small"
        />
        <LabelWithHint
          label={group.label}
          hint={expandable ? undefined : group.children[0]?.hint}
        />
        <Box width={28} ml={1} display="flex" justifyContent="center">
          {expandable && (
            <IconButton
              size="small"
              sx={{ p: 0.5, color: 'text.secondary' }}
              onClick={(e) => {
                e.stopPropagation();
                onToggleExpanded(group.value);
              }}
              data-cy="activity-filter-group-expand"
            >
              <ChevronDown
                width={18}
                height={18}
                style={{
                  transition: 'transform 0.15s',
                  transform: isOpen ? 'rotate(180deg)' : undefined,
                }}
              />
            </IconButton>
          )}
        </Box>
      </MenuItem>
    );
    if (isOpen) {
      group.children.forEach((child) => {
        const childCount = countSelected(child.types);
        const childFull = childCount === child.types.length;
        items.push(
          <MenuItem
            key={child.value}
            value=""
            sx={{ pl: 5 }}
            onClick={() => setTypes(child.types, !childFull)}
          >
            <Checkbox
              checked={childFull}
              indeterminate={!childFull && childCount > 0}
              size="small"
            />
            <LabelWithHint label={child.label} hint={child.hint} small />
          </MenuItem>
        );
      });
    }
  });

  return items;
};
