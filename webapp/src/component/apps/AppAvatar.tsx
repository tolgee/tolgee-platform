import { styled } from '@mui/material';

import { AutoAvatar } from 'tg.component/common/avatar/AutoAvatar';
import { AppIcon } from 'tg.component/apps/AppIcon';

const StyledTile = styled('div')`
  border-radius: 10px;
  background: ${({ theme }) => theme.palette.tile.background};
  border: 1px solid ${({ theme }) => theme.palette.divider};
  display: grid;
  place-items: center;
  flex: none;
  overflow: hidden;
`;

type Props = {
  /** Manifest icon: emoji, native icon name, or an absolute image URL. */
  icon?: string | null;
  /** Seeds the generated fallback when the manifest declares no icon. */
  name: string;
  /** Stable identity for the fallback, so it doesn't change when the app is renamed. */
  appId: string;
  size?: number;
};

/**
 * The app's logo tile. With no manifest icon it falls back to the same generated
 * initials avatar projects and organizations get.
 */
export const AppAvatar = ({ icon, name, appId, size = 42 }: Props) => {
  return (
    <StyledTile style={{ width: size, height: size }}>
      {icon ? (
        <AppIcon icon={icon} size={Math.round(size * 0.55)} />
      ) : (
        <AutoAvatar
          entityId={`APP-${appId}`}
          size={size}
          ownerName={name}
          ownerType="PROJECT"
        />
      )}
    </StyledTile>
  );
};
