import { debounce, styled, Tooltip } from '@mui/material';
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
  useMemo,
  useRef,
  useState,
} from 'react';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import clsx from 'clsx';
import { CELL_SHOW_ON_HOVER } from 'tg.views/projects/translations/cell/styles';
import { useTranslate } from '@tolgee/react';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { useEnabledFeatures } from 'tg.globalContext/helpers';

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

  .translation-label {
    cursor: default;
  }
`;

const TooltipStyledList = styled(StyledList)`
  flex-direction: column;
  padding: 4px 0;
  gap: 8px;
`;

const TooltipStyledListItem = styled('div')`
  display: flex;
`;

const HiddenMeasure = styled(StyledList)`
  visibility: hidden;
  position: absolute;
  pointer-events: none;
  height: 0;
  overflow: hidden;
  width: unset;
`;

const StyledMoreTranslationLabel = styled(StyledTranslationLabel)`
  background-color: ${({ theme }) =>
    theme.palette.tokens._components.chip.defaultFill};
  color: ${({ theme }) => theme.palette.text.primary};
  min-width: 37px;
`;

type Props = {
  labels: LabelModel[] | undefined;
  className: string;
  onSelect?: (label: LabelModel) => void;
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
  const { isEnabled } = useEnabledFeatures();
  const labelsEnabled = isEnabled('TRANSLATION_LABELS');
  if (!labelsEnabled) {
    return null;
  }
  const { t } = useTranslate();

  const containerRef = useRef<HTMLDivElement | null>(null);
  const labelControlRef = useRef<HTMLDivElement | null>(null);
  const measureRef = useRef<HTMLDivElement | null>(null);
  const [visibleCount, setVisibleCount] = useState(labels?.length || 0);
  const [labeledMore, setLabeledMore] = useState(false);

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

    const moreWidth = labels ? measured[labels.length]?.offsetWidth : 0;

    let used = 0;
    let fit = labels.length;

    for (let i = 0; i < labels.length; i++) {
      const chipWidth = measured[i].offsetWidth + 6; // 6px gap between chips
      const remaining = labels.length - i - 1;
      const needsMore = remaining > 0;

      const required = used + chipWidth + (needsMore ? moreWidth : 0) + 1; // +1 for the decimal pixel rounding

      if (required > available) {
        fit = Math.max(0, i);
        break;
      }
      used += chipWidth;
    }

    if (fit !== visibleCount) setVisibleCount(fit);
    setLabeledMore(fit == 0);
  }, [labels, canAssignLabels, visibleCount]);

  const debouncedRecalculate = useMemo(
    () => debounce(recalculate, 100),
    [recalculate]
  );

  useLayoutEffect(() => {
    recalculate();
  }, [labels, recalculate]);

  useEffect(() => {
    if (!containerRef.current) return;
    const ro = new ResizeObserver(recalculate);
    ro.observe(containerRef.current);
    return () => {
      ro.disconnect();
      debouncedRecalculate.clear();
    };
  }, [debouncedRecalculate]);

  const overflowCount = labels?.length - visibleCount;

  return (
    <StyledLabels className={className}>
      <StyledList className="translation-labels-list" ref={containerRef}>
        {labels?.slice(0, visibleCount).map((label) => (
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
                {labels?.slice(visibleCount).map((hiddenLabel) => (
                  <TooltipStyledListItem key={hiddenLabel.id}>
                    <TranslationLabel
                      label={hiddenLabel}
                      tooltip={hiddenLabel.description}
                      onClick={(e) => e.stopPropagation()}
                      onDelete={
                        onDelete && canAssignLabels
                          ? () => onDelete(hiddenLabel.id)
                          : undefined
                      }
                    />
                  </TooltipStyledListItem>
                ))}
              </TooltipStyledList>
            }
            placement="bottom"
          >
            <StyledMoreTranslationLabel>
              {labeledMore
                ? t('translations_list_labels_more_label_full', {
                    count: overflowCount,
                  })
                : t('translations_list_labels_more_label', {
                    count: overflowCount,
                  })}
            </StyledMoreTranslationLabel>
          </Tooltip>
        )}
        {canAssignLabels && onSelect && (
          <LabelControl
            className={clsx('clickable', CELL_SHOW_ON_HOVER)}
            ref={labelControlRef}
            onSelect={onSelect}
            existing={labels}
          />
        )}
      </StyledList>
      <HiddenMeasure ref={measureRef} className="hidden-measure">
        {labels?.map((label) => (
          <TranslationLabel
            label={label}
            key={label.id}
            tooltip={label.description}
          />
        ))}
        <StyledMoreTranslationLabel>
          {t('translations_list_labels_more_label', {
            count: 99,
          })}
        </StyledMoreTranslationLabel>
      </HiddenMeasure>
    </StyledLabels>
  );
};
