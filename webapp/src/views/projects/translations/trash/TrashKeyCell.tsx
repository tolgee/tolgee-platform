import React, { useState } from 'react';
import { Checkbox, Menu, MenuItem, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { ArrowDropDown } from 'tg.component/CustomIcons';
import { Tag } from '../Tags/Tag';
import { KeyCellContent } from '../KeyCellContent';
import { TrashedKeyModel } from './TrashRow';

const StyledKeyCell = styled('div')`
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto auto auto auto 1fr;
  grid-template-areas:
    'checkbox  key          '
    '.         description  '
    '.         screenshots  '
    '.         tags         '
    '.         .            ';
  position: relative;
  outline: 0;
  overflow: hidden;
  min-width: 0;
`;

const StyledNamespaceChip = styled('div')`
  display: flex;
  align-items: center;
  cursor: pointer;
  background: ${({ theme }) => theme.palette.background.default};
  padding: ${({ theme }) => theme.spacing(0, 1.5, 0, 1.5)};
  padding-bottom: 1px;
  height: 24px;
  position: absolute;
  top: -12px;
  left: 2px;
  border-radius: 12px;
  box-shadow: ${({ theme }) =>
    theme.palette.mode === 'dark'
      ? '0px 0px 7px -1px #000000'
      : '0px 0px 7px -2px #00000097'};
  z-index: 1;
  max-width: 100%;
  font-size: 14px;
  &:hover {
    background: ${({ theme }) => theme.palette.emphasis[50]};
  }
`;

const StyledNamespaceContent = styled('div')`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const StyledMoreArrow = styled('div')`
  display: flex;
  align-items: center;
  padding-left: 2px;
  margin-right: ${({ theme }) => theme.spacing(-0.5)};
`;

const StyledCheckbox = styled(Checkbox)`
  grid-area: checkbox;
  width: 38px;
  height: 38px;
  margin: 3px -9px -9px 3px;
`;

const StyledScreenshots = styled('div')`
  grid-area: screenshots;
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  padding: 0px 12px 8px 12px;
  overflow: hidden;
`;

const StyledScreenshotBox = styled('div')`
  overflow: hidden;
  position: relative;
  border-radius: 4px;
  background: ${({ theme }) => theme.palette.tokens.text._states.selected};

  &::after {
    content: '';
    position: absolute;
    top: 0px;
    left: 0px;
    right: 0px;
    bottom: 0px;
    border-radius: 4px;
    border: 1px solid ${({ theme }) => theme.palette.tokens.border.primary};
    pointer-events: none;
  }
`;

const StyledScreenshotImg = styled('img')`
  width: 100%;
  height: 100%;
  object-fit: contain;
`;

const StyledTags = styled('div')`
  grid-area: tags;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  min-width: 0;
  & > * {
    margin: 0 6px 3px 0;
  }
  margin: 0px 12px 12px 12px;
  position: relative;
`;

type Props = {
  data: TrashedKeyModel;
  selected: boolean;
  onToggle: () => void;
  showNamespace: boolean;
  onFilterNamespace?: (namespace: string) => void;
};

export const TrashKeyCell: React.FC<Props> = ({
  data,
  selected,
  onToggle,
  showNamespace,
  onFilterNamespace,
}) => {
  const { t } = useTranslate();
  const [nsMenuAnchor, setNsMenuAnchor] = useState<HTMLElement | null>(null);
  const tags = data.tags ?? [];

  return (
    <>
      {showNamespace && (
        <>
          <StyledNamespaceChip
            onClick={(e) => setNsMenuAnchor(e.currentTarget)}
          >
            <StyledNamespaceContent>{data.namespace}</StyledNamespaceContent>
            <StyledMoreArrow>
              <ArrowDropDown fontSize="small" />
            </StyledMoreArrow>
          </StyledNamespaceChip>
          {nsMenuAnchor && (
            <Menu
              anchorEl={nsMenuAnchor}
              open={Boolean(nsMenuAnchor)}
              onClose={() => setNsMenuAnchor(null)}
            >
              <MenuItem
                onClick={() => {
                  onFilterNamespace?.(data.namespace!);
                  setNsMenuAnchor(null);
                }}
              >
                {t('namespace_menu_filter', { namespace: data.namespace })}
              </MenuItem>
            </Menu>
          )}
        </>
      )}
      <StyledKeyCell style={showNamespace ? { paddingTop: 14 } : undefined}>
        <StyledCheckbox
          size="small"
          checked={selected}
          onChange={onToggle}
          data-cy="trash-row-checkbox"
        />
        <KeyCellContent keyName={data.name} description={data.description} />
        {data.screenshots?.length > 0 && (
          <StyledScreenshots>
            {data.screenshots.map((sc) => {
              const w = 100;
              const h =
                sc.width && sc.height
                  ? Math.min(w / (sc.width / sc.height), 100)
                  : 100;
              return (
                <StyledScreenshotBox
                  key={sc.id}
                  style={{ width: w, height: h }}
                >
                  <StyledScreenshotImg src={sc.thumbnailUrl} alt="" />
                </StyledScreenshotBox>
              );
            })}
          </StyledScreenshots>
        )}
        {tags.length > 0 && (
          <StyledTags>
            {tags.map((tag) => (
              <Tag key={tag.id} name={tag.name} />
            ))}
          </StyledTags>
        )}
      </StyledKeyCell>
    </>
  );
};
