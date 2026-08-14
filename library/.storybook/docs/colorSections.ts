import type { PaletteGroup } from './Swatches';

// Single source for what the Colors page lists explicitly, so `RemainingColors` cannot fall out of
// step with the sections above it.
export const COLOR_SECTIONS = {
  brand: ['colors'],
  tokens: ['tokens'],
  surfaces: ['background', 'tile', 'cell', 'navbar'],
  editorAndActivity: ['editor', 'activity', 'marker'],
  banners: ['topBanner', 'exampleBanner', 'tipsBanner', 'revisionFilterBanner'],
} satisfies Record<string, PaletteGroup[]>;

export const CURATED_GROUPS: PaletteGroup[] =
  Object.values(COLOR_SECTIONS).flat();
