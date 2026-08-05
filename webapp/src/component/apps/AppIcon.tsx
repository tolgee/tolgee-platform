import React, { lazy, Suspense } from 'react';
import * as CustomIcons from 'tg.component/CustomIcons';

type IconComponent = React.ComponentType<{
  width?: number | string;
  height?: number | string;
}>;

type IconModule = { default: IconComponent };

/**
 * Untitled UI exports ~1200 icon components and a manifest names one at runtime. Reaching
 * them through per-icon lazy chunks keeps the package out of the main bundle — a namespace
 * import of it pins every icon into whatever chunk holds the project menu.
 */
const NATIVE_ICON_LOADERS = new Map<string, () => Promise<IconModule>>(
  Object.entries(
    import.meta.glob<IconModule>([
      '/node_modules/@untitled-ui/icons-react/build/esm/*.js',
      '!/node_modules/@untitled-ui/icons-react/build/esm/index.js',
    ])
  ).map(
    ([path, load]) =>
      [path.slice(path.lastIndexOf('/') + 1, -'.js'.length), load] as const
  )
);

const CUSTOM_ICONS = new Map<string, IconComponent>(
  Object.entries(CustomIcons) as [string, IconComponent][]
);

const lazyNativeIcons = new Map<string, IconComponent>();

function resolveIconComponent(name: string): IconComponent | undefined {
  const custom = CUSTOM_ICONS.get(name);
  if (custom) {
    return custom;
  }
  const alreadyLazy = lazyNativeIcons.get(name);
  if (alreadyLazy) {
    return alreadyLazy;
  }
  const loader = NATIVE_ICON_LOADERS.get(name);
  if (!loader) {
    return undefined;
  }
  const component = lazy(loader) as IconComponent;
  lazyNativeIcons.set(name, component);
  return component;
}

/**
 * The size @untitled-ui/icons-react renders at when given no width/height. The emoji
 * fallback has to match it explicitly, otherwise an emoji icon comes out smaller than
 * a native one sitting next to it.
 */
const NATIVE_ICON_DEFAULT_SIZE = 24;

type Props = {
  /** Manifest icon string. Either a Tolgee native icon name or an emoji. */
  icon?: string | null;
  /** px size for the icon. Omit to render at the same size as any other native icon. */
  size?: number;
  /** CSS font-size for the emoji / text fallback. Defaults to `size`. */
  fontSize?: string | number;
};

/**
 * Renders a plugin manifest's `icon` field. If the string matches a name in
 * the native icon registry (@untitled-ui/icons-react + tg.component/CustomIcons),
 * the matching React component is rendered. Otherwise the string is rendered as
 * text — preserving the emoji path and producing a self-explanatory fallback for
 * unknown names.
 */
export const AppIcon = ({ icon, size, fontSize }: Props) => {
  if (!icon) return null;

  const boxSize = size ?? NATIVE_ICON_DEFAULT_SIZE;
  const Component = resolveIconComponent(icon);

  if (Component) {
    const sizeProps = size === undefined ? {} : { width: size, height: size };
    return (
      <Suspense
        fallback={
          <span
            style={{ display: 'inline-block', width: boxSize, height: boxSize }}
          />
        }
      >
        <Component {...sizeProps} />
      </Suspense>
    );
  }

  return (
    <span
      style={{
        fontSize: fontSize ?? `${boxSize}px`,
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
