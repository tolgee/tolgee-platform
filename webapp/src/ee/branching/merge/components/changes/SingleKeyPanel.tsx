import { FC } from 'react';
import clsx from 'clsx';
import {
  KeyFooterToggle,
  KeyHeader,
  KeyPanel,
  KeyWrapper,
} from './KeyPanelBase';
import { KeyTranslations } from './KeyTranslations';
import { MergeKeyHeader } from './MergeKeyHeader';
import { BranchMergeKeyModel } from '../../types';

type Props = {
  keyData: BranchMergeKeyModel;
  changedTranslations?: string[];
  showAll?: boolean;
  onToggleShowAll?: () => void;
  hideAllWhenFalse?: boolean;
  variant?: 'added' | 'deleted';
  toggleLabels?: {
    showAll: string;
    showLess: string;
  };
};

export const SingleKeyPanel: FC<Props> = ({
  keyData,
  changedTranslations,
  showAll,
  onToggleShowAll,
  hideAllWhenFalse,
  variant,
  toggleLabels,
}) => (
  <KeyWrapper className={clsx(variant)}>
    <KeyPanel>
      <KeyHeader>
        <MergeKeyHeader data={keyData} variant={variant} />
      </KeyHeader>
      <KeyTranslations
        keyData={keyData}
        changedTranslations={changedTranslations}
        showAll={showAll}
        hideAllWhenFalse={hideAllWhenFalse}
      />
    </KeyPanel>
    {onToggleShowAll && (
      <KeyFooterToggle
        showAll={showAll}
        onToggleShowAll={onToggleShowAll}
        toggleLabels={toggleLabels}
      />
    )}
  </KeyWrapper>
);
