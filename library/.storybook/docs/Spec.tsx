import {
  Children,
  cloneElement,
  isValidElement,
  useCallback,
  useEffect,
  useRef,
  useState,
  type ReactElement,
  type ReactNode,
} from 'react';
import { Box, ThemeProvider } from '@mui/material';
import { DocsTheme } from './DocsTheme';
import { SHIPPED_DARK, SHIPPED_LIGHT, useThemes } from './themes';

type Measurement = {
  label: string;
  value: string;
  shipped?: string;
};

const px = (n: number) => `${Math.round(n * 100) / 100}px`;

// These pages render at the sizes the product is meant to have. The app still ships
// htmlFontSize: 15, so the same sample is measured against the shipped theme too and the block
// reports the gap rather than anyone doing the arithmetic. See General Findings.

const boxOf = (el: Element) => {
  const r = el.getBoundingClientRect();
  return `${Math.round(r.width * 100) / 100} × ${Math.round(r.height * 100) / 100}`;
};

const shorthand = (top: string, right: string, bottom: string, left: string) =>
  top === right && right === bottom && bottom === left
    ? top
    : top === bottom && right === left
      ? `${top} ${right}`
      : `${top} ${right} ${bottom} ${left}`;

/**
 * Measures what the browser actually laid out, at render time, so the numbers cannot
 * drift away from the component the way written-down ones do.
 */
export const Spec = ({
  children,
  glyphSelector = 'svg',
  extra,
}: {
  children: ReactNode;
  glyphSelector?: string;
  extra?: (root: HTMLElement) => Measurement[];
}) => {
  const hostRef = useRef<HTMLDivElement>(null);
  const shadowRef = useRef<HTMLDivElement>(null);
  const { dark } = useThemes();
  const [rows, setRows] = useState<Measurement[] | null>(null);
  const [outline, setOutline] = useState<DOMRect[] | null>(null);

  const measure = useCallback(() => {
    const host = hostRef.current;
    const root = host?.firstElementChild as HTMLElement | undefined;
    if (!host || !root) return;

    const cs = getComputedStyle(root);
    const rect = root.getBoundingClientRect();
    const glyph = root.matches(glyphSelector)
      ? root
      : root.querySelector<SVGElement>(glyphSelector);

    const out: Measurement[] = [{ label: 'box', value: boxOf(root) }];

    const padding = shorthand(
      cs.paddingTop,
      cs.paddingRight,
      cs.paddingBottom,
      cs.paddingLeft,
    );
    if (padding !== '0px') out.push({ label: 'padding', value: padding });

    if (cs.borderTopWidth !== '0px') {
      out.push({
        label: 'border',
        value: `${cs.borderTopWidth} ${cs.borderTopStyle}`,
      });
    }
    if (cs.borderRadius !== '0px') {
      out.push({ label: 'radius', value: cs.borderRadius });
    }
    if (glyph) {
      out.push({ label: 'glyph', value: boxOf(glyph) });
    }
    const minSide = Math.min(rect.width, rect.height);
    out.push({
      label: 'hit',
      value: minSide >= 24 ? `${px(minSide)} \u2713` : `${px(minSide)} \u2717`,
    });

    // the same sample under the theme the app still ships, so the block can report the gap
    const shadowRoot = shadowRef.current?.firstElementChild as
      | HTMLElement
      | undefined;
    const shipped: Record<string, string> = {};
    if (shadowRoot) {
      shipped.box = boxOf(shadowRoot);
      const shadowGlyph = shadowRoot.matches(glyphSelector)
        ? shadowRoot
        : shadowRoot.querySelector<SVGElement>(glyphSelector);
      if (shadowGlyph) shipped.glyph = boxOf(shadowGlyph);
      const shadowRect = shadowRoot.getBoundingClientRect();
      shipped.hit = px(Math.min(shadowRect.width, shadowRect.height));
    }

    setRows(
      [...out, ...(extra ? extra(root) : [])].map((r) => {
        const target = shipped[r.label];
        const value = r.label === 'hit' ? r.value.split(' ')[0] : r.value;
        return target && target !== value ? { ...r, shipped: target } : r;
      }),
    );

    const hostRect = host.getBoundingClientRect();
    const rel = (r: DOMRect) =>
      new DOMRect(r.x - hostRect.x, r.y - hostRect.y, r.width, r.height);
    setOutline(
      glyph ? [rel(rect), rel(glyph.getBoundingClientRect())] : [rel(rect)],
    );
  }, [glyphSelector, extra]);

  useEffect(() => {
    // Two frames: one for layout, one for the fonts and transitions to settle.
    const id = requestAnimationFrame(() => requestAnimationFrame(measure));
    return () => cancelAnimationFrame(id);
  }, [measure]);

  // Without the theme the sample renders against Material UI's defaults, which is not
  // what the product ships — htmlFontSize alone moves a small checkbox by 1.33px.
  return (
    <DocsTheme>
      <Box
        sx={{
          display: 'flex',
          gap: 2,
          alignItems: 'center',
          flexWrap: 'wrap',
          py: 0.5,
        }}
      >
        <Box
          sx={{ position: 'relative', display: 'inline-flex', flexShrink: 0 }}
          ref={hostRef}
        >
          {Children.map(children, (child) =>
            isValidElement(child) ? cloneElement(child as ReactElement) : child,
          )}
          {outline?.map((r, i) => (
            <Box
              key={i}
              aria-hidden
              sx={{
                position: 'absolute',
                pointerEvents: 'none',
                left: r.x,
                top: r.y,
                width: r.width,
                height: r.height,
                outline: '1px dashed',
                outlineColor: i === 0 ? 'error.main' : 'info.main',
              }}
            />
          ))}
        </Box>

        <Box
          aria-hidden
          ref={shadowRef}
          sx={{
            position: 'absolute',
            left: -9999,
            top: 0,
            visibility: 'hidden',
          }}
        >
          <ThemeProvider theme={dark ? SHIPPED_DARK : SHIPPED_LIGHT}>
            {Children.map(children, (child) =>
              isValidElement(child)
                ? cloneElement(child as ReactElement)
                : child,
            )}
          </ThemeProvider>
        </Box>

        <Box
          sx={{
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'baseline',
            columnGap: 1.5,
            rowGap: 0.25,
            typography: 'caption',
            fontFamily: 'monospace',
          }}
        >
          {rows?.map((r, i) => (
            <Box
              key={r.label}
              sx={{ display: 'flex', gap: 0.5, alignItems: 'baseline' }}
            >
              {i > 0 && (
                <Box component="span" sx={{ color: 'text.disabled', mr: 0.5 }}>
                  ·
                </Box>
              )}
              <Box component="span" sx={{ color: 'text.secondary' }}>
                {r.label}
              </Box>
              <Box component="span">{r.value}</Box>
              {r.shipped && (
                <Box
                  component="span"
                  sx={{ color: 'error.main' }}
                  title="what the app renders today"
                >
                  (app: {r.shipped})
                </Box>
              )}
            </Box>
          ))}
          {!rows && (
            <Box component="span" sx={{ color: 'text.secondary' }}>
              measuring…
            </Box>
          )}
        </Box>
      </Box>
    </DocsTheme>
  );
};
