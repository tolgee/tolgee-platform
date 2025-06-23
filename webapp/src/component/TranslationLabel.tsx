import { styled, Tooltip } from '@mui/material';
import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import clsx from 'clsx';
import { CloseButton } from 'tg.component/common/buttons/CloseButton';
import { getBackgroundColor, getTextColor } from 'tg.globalContext/colorUtils';

type LabelModel = components['schemas']['LabelModel'];

export const StyledTranslationLabel = styled('div')<{ color?: string }>`
  background-color: ${({ color, theme }) => getBackgroundColor(theme, color)};
  border-radius: 12px;
  color: ${({ color, theme }) => getTextColor(theme, color)};
  padding: 0 10px;
  font-size: 14px;
  line-height: 18px;
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  overflow: hidden;
  white-space: nowrap;
  height: 24px;
`;

const ToolTipWrapper = styled('div')`
  display: flex;
`;

export const TranslationLabel: React.FC<{
  label: LabelModel;
  tooltip?: string;
  children?: React.ReactNode;
  className?: string;
  onClick?: (e: React.MouseEvent<HTMLDivElement>) => void;
  onDelete?: (labelId: number) => void;
}> = ({ label, children, tooltip, className, onClick, onDelete, ...rest }) => {
  const labelContent = (
    <StyledTranslationLabel
      color={label.color}
      data-cy="translation-label"
      className={clsx(className, 'translation-label')}
      onClick={onClick}
      {...rest}
    >
      {children || label.name}
    </StyledTranslationLabel>
  );

  const content = onDelete ? (
    <CloseButton
      data-cy="translation-label-delete"
      onClose={(e) => {
        e.stopPropagation();
        onDelete(label.id);
      }}
      xs
    >
      {labelContent}
    </CloseButton>
  ) : (
    labelContent
  );

  return tooltip ? (
    <ToolTipWrapper>
      <Tooltip title={tooltip}>
        <div>{content}</div>
      </Tooltip>
    </ToolTipWrapper>
  ) : (
    content
  );
};
