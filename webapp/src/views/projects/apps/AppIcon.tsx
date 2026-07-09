import * as UntitledIcons from '@untitled-ui/icons-react';
import * as CustomIcons from 'tg.component/CustomIcons';

type IconComponent = React.ComponentType<{
  width?: number | string;
  height?: number | string;
}>;

const REGISTRY: Record<string, IconComponent> = {
  ...(UntitledIcons as unknown as Record<string, IconComponent>),
  ...(CustomIcons as unknown as Record<string, IconComponent>),
};

type Props = {
  /** Manifest icon string. Either a Tolgee native icon name or an emoji. */
  icon?: string | null;
  /** px size for native icons. */
  size?: number;
  /** CSS font-size for the emoji / text fallback. */
  fontSize?: string | number;
};

/**
 * Renders a plugin manifest's `icon` field. If the string matches a name in
 * the merged native icon registry (@untitled-ui/icons-react +
 * tg.component/CustomIcons), the matching React component is rendered.
 * Otherwise the string is rendered as text — preserving the emoji path
 * and producing a self-explanatory fallback for unknown names.
 */
export const AppIcon = ({ icon, size = 18, fontSize }: Props) => {
  if (!icon) return null;
  const Component = REGISTRY[icon];
  if (typeof Component === 'function') {
    return <Component width={size} height={size} />;
  }
  return (
    <span
      style={{
        fontSize: fontSize ?? '1em',
        lineHeight: 1,
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      {icon}
    </span>
  );
};
