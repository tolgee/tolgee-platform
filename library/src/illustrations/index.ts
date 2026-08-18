/// <reference types="vite-plugin-svgr/client" />
// Illustrations, not icons: they carry their own size and composition, appear once on a screen and
// are never interchangeable with a glyph. The boundary is measurable — anything with a viewBox
// larger than 24 belongs here.

export { default as GlossaryEmpty } from './svg/glossary-empty.svg?react';
export { default as SelfHostedPlaceholder } from './svg/selfHostedPlaceholder.svg?react';
