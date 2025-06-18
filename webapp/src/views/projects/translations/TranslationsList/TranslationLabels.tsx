import { styled, useTheme, Tooltip } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import {
  StyledTranslationLabel,
  TranslationLabel,
} from 'tg.component/TranslationLabel';
import { LabelControl } from 'tg.views/projects/translations/TranslationsList/Label/LabelControl';
import React, {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from 'react';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import clsx from 'clsx';
import { CELL_SHOW_ON_HOVER } from 'tg.views/projects/translations/cell/styles';
import { useTranslate } from '@tolgee/react';
import { stopBubble } from 'tg.fixtures/eventHandler';

type LabelModel = components['schemas']['LabelModel'];

const StyledLabels = styled('div')`
  display: flex;
  grid-area: labels;
  align-items: center;
`;

const StyledList = styled('div')`
  display: flex;
  gap: 6px;
  width: 100%;

  & > div:last-child {
    margin-right: 6px;
  }

  & > div {
    flex-shrink: 0;
  }

  .translation-label {
    cursor: default;
  }
`;

const TooltipStyledList = styled(StyledList)`
  flex-direction: column;
  padding: 4px 0;
  gap: 8px;
`;

const HiddenMeasure = styled(StyledList)`
  visibility: hidden;
  position: absolute;
  pointer-events: none;
  height: 0;
  overflow: hidden;
`;

const StyledMoreTranslationLabel = styled(StyledTranslationLabel)`
  background-color: ${({ theme }) =>
    theme.palette.tokens._components.chip.defaultFill};
  color: ${({ theme }) => theme.palette.text.primary};
`;

type Props = {
  labels: LabelModel[] | undefined;
  className: string;
  onSelect?: (labelId: number) => void;
  onDelete?: (labelId: number) => void;
};

export const TranslationLabels = ({
  labels = [],
  className,
  onSelect,
  onDelete,
}: Props) => {
  const { satisfiesPermission } = useProjectPermissions();
  const canAssignLabels = satisfiesPermission('translation-labels.assign');
  const theme = useTheme();
  const { t } = useTranslate();

  const containerRef = useRef<HTMLDivElement | null>(null);
  const labelControlRef = useRef<HTMLDivElement | null>(null);
  const measureRef = useRef<HTMLDivElement | null>(null);
  const [visibleCount, setVisibleCount] = useState(labels.length);

  const recalculate = useCallback(() => {
    if (!labels) return;
    if (!containerRef.current || !measureRef.current) return;
    const controlEl = labelControlRef.current;
    const containerWidth = containerRef.current.offsetWidth;
    const controlWidth =
      canAssignLabels && controlEl ? controlEl.offsetWidth : 0;
    const available = containerWidth - controlWidth;

    const measured = Array.from(measureRef.current.children) as HTMLElement[];
    if (!measured.length) return;

    const moreWidth = measured[labels.length]?.offsetWidth || 0;

    let used = 0;
    let fit = labels.length;

    for (let i = 0; i < labels.length; i++) {
      const chipWidth = measured[i].offsetWidth;
      const remaining = labels.length - i - 1;
      const needsMore = remaining > 0;

      const required = used + chipWidth + (needsMore ? moreWidth : 0) + 12; // 6px gap

      if (required > available) {
        fit = Math.max(1, i - 1);
        break;
      }
      used += chipWidth;
    }

    if (fit !== visibleCount) setVisibleCount(fit);
  }, [labels, canAssignLabels, visibleCount]);

  useLayoutEffect(() => {
    recalculate();
  }, [labels, recalculate]);

  useEffect(() => {
    if (!containerRef.current) return;
    const ro = new ResizeObserver(recalculate);
    ro.observe(containerRef.current);
    return () => ro.disconnect();
  }, [recalculate]);

  const overflowCount = labels.length - visibleCount;

  return (
    <StyledLabels className={className}>
      <StyledList className="translation-labels-list" ref={containerRef}>
        {labels.slice(0, visibleCount).map((label) => (
          <TranslationLabel
            label={label}
            key={label.id}
            tooltip={label.description}
            onClick={(e) => e.stopPropagation()}
            onDelete={
              onDelete && canAssignLabels ? () => onDelete(label.id) : undefined
            }
          />
        ))}

        {overflowCount > 0 && (
          <Tooltip
            onClick={(e) => stopBubble<HTMLElement>(() => {})(e)}
            title={
              <TooltipStyledList>
                {labels.slice(visibleCount).map((hiddenLabel) => (
                  <TranslationLabel
                    label={hiddenLabel}
                    key={hiddenLabel.id}
                    tooltip={hiddenLabel.description}
                    onClick={(e) => e.stopPropagation()}
                    onDelete={
                      onDelete && canAssignLabels
                        ? () => onDelete(hiddenLabel.id)
                        : undefined
                    }
                  />
                ))}
              </TooltipStyledList>
            }
            placement="bottom"
          >
            <StyledMoreTranslationLabel>
              {t('translations_list_labels_more_label', {
                count: overflowCount,
              })}
            </StyledMoreTranslationLabel>
          </Tooltip>
        )}
        {canAssignLabels && (
          <LabelControl
            className={clsx('clickable', CELL_SHOW_ON_HOVER)}
            ref={labelControlRef}
            onSelect={onSelect}
            existing={labels}
          />
        )}
      </StyledList>
      <HiddenMeasure ref={measureRef} className="hidden-measure">
        {labels.map((label) => (
          <TranslationLabel
            label={label}
            key={label.id}
            tooltip={label.description}
          />
        ))}
        <TranslationLabel
          label={{ name: `+99`, color: 'gray' } as LabelModel}
        />
      </HiddenMeasure>
    </StyledLabels>
  );
};
