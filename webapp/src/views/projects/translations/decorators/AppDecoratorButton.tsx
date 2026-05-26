import { Badge, styled } from '@mui/material';

import { ControlsButton } from '../cell/ControlsButton';
import { CELL_SHOW_ON_HOVER } from '../cell/styles';
import { useTranslationsActions } from '../context/TranslationsContext';
import { ResolvedDecorator } from './useAppDecorators';
import { requestPanelReveal } from './panelRevealEvent';
import { requestKeyDialogOpen } from './keyDialogEvent';
import { AppIcon } from '../../apps/AppIcon';

const StyledBadge = styled(Badge)`
  & .MuiBadge-badge {
    font-size: 10px;
    height: unset;
    padding: 3px 3px;
    display: flex;
    min-width: 16px;
  }
`;

type Props = {
  decorator: ResolvedDecorator;
  keyId: number;
  languageTag?: string;
};

export const AppDecoratorButton = ({
  decorator,
  keyId,
  languageTag,
}: Props) => {
  const { setEdit } = useTranslationsActions();

  const onClick = () => {
    if (decorator.type === 'link') {
      window.open(decorator.url, '_blank', 'noopener,noreferrer');
    } else if (decorator.type === 'panel') {
      // Focus the cell (sets cursor + opens side panel) so the
      // tools-panel renders panels for this translation. Then request
      // the reveal so the plugin's panel is the one expanded + scrolled
      // into view.
      setEdit({ keyId, language: languageTag });
      const id = `app:${decorator.installId}:${decorator.panelKey}`;
      requestPanelReveal(id);
    } else if (decorator.type === 'tab') {
      requestKeyDialogOpen({
        keyId,
        initialTab: `app:${decorator.installId}:${decorator.tabKey}`,
      });
    }
  };

  const hoverOnly = decorator.visibility === 'on-hover';
  const showBadge = typeof decorator.count === 'number' && decorator.count > 0;

  const iconNode = (
    <AppIcon icon={decorator.icon} size={18} fontSize="1.05em" />
  );

  return (
    <ControlsButton
      onClick={onClick}
      tooltip={decorator.tooltip}
      data-cy="translation-app-decorator"
      data-cy-action-key={decorator.actionKey}
      data-cy-visibility={decorator.visibility}
      className={hoverOnly ? CELL_SHOW_ON_HOVER : undefined}
    >
      {showBadge ? (
        <StyledBadge
          badgeContent={decorator.count}
          color="primary"
          data-cy="translation-app-decorator-badge"
        >
          {iconNode}
        </StyledBadge>
      ) : (
        iconNode
      )}
    </ControlsButton>
  );
};
