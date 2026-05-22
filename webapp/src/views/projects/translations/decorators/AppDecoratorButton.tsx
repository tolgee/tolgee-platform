import { ControlsButton } from '../cell/ControlsButton';
import { ResolvedDecorator } from './useAppDecorators';
import { requestPanelReveal } from './panelRevealEvent';
import { requestKeyDialogOpen } from './keyDialogEvent';

type Props = {
  decorator: ResolvedDecorator;
  keyId: number;
};

export const AppDecoratorButton = ({ decorator, keyId }: Props) => {
  const onClick = () => {
    if (decorator.type === 'link') {
      window.open(decorator.url, '_blank', 'noopener,noreferrer');
    } else if (decorator.type === 'panel') {
      const id = `app:${decorator.installId}:${decorator.panelKey}`;
      requestPanelReveal(id);
    } else if (decorator.type === 'tab') {
      requestKeyDialogOpen({
        keyId,
        initialTab: `app:${decorator.installId}:${decorator.tabKey}`,
      });
    }
  };

  return (
    <ControlsButton
      onClick={onClick}
      tooltip={decorator.tooltip}
      data-cy="translation-app-decorator"
      data-cy-action-key={decorator.actionKey}
    >
      <span style={{ fontSize: '1.05em', lineHeight: 1 }}>
        {decorator.icon}
      </span>
    </ControlsButton>
  );
};
