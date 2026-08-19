// Generated from webapp/src on 2026-08-18. Regenerate rather than edit by hand.

export type Decl = {
  prop: string;
  /** Literal CSS value. */
  value?: string;
  /** Dotted path into `theme.palette`, with anything written before it kept in `prefix`. */
  palette?: string;
  prefix?: string;
  /** Arguments handed to `theme.spacing()`. */
  spacing?: number[];
};

export type RestyledButton = {
  component: string;
  name: string;
  file: string;
  area: string;
  /** What the thing is once you look at it, rather than what it was built from. */
  kind: string;
  structural: string[];
  decls: Decl[];
  users: number;
};

export const RESTYLED_BUTTONS: RestyledButton[] = [
  {
    component: 'StyledButton',
    name: 'ButtonToggle',
    file: 'component/ButtonToggle.tsx',
    area: 'Translations filters',
    kind: 'Toggle',
    structural: [
      'background-color',
      'border-radius',
      'box-shadow',
      'color',
      'font-size',
      'font-weight',
      'min-height',
      'text-transform',
    ],
    decls: [
      { prop: 'margin-left', value: '8px' },
      { prop: 'padding', value: '4px 8px' },
      { prop: 'font-size', value: '13px' },
      { prop: 'align-self', value: 'center' },
      { prop: 'min-height', value: '0px' },
      { prop: 'text-transform', value: 'none' },
      { prop: 'font-style', value: 'normal' },
      { prop: 'font-weight', value: '500' },
      {
        prop: 'background-color',
        palette: 'tokens._components.buttonToggle.enabled',
        prefix: '',
      },
      {
        prop: 'color',
        palette: 'tokens._components.buttonToggle.textEnabled',
        prefix: '',
      },
      { prop: 'box-shadow', value: '0px 2px 8px 0px rgba(0, 0, 0, 0.2)' },
      { prop: 'border-radius', value: '4px' },
      { prop: 'line-height', value: 'normal' },
    ],
    users: 1,
  },
  {
    component: 'StyledButton',
    name: 'ChipButton',
    file: 'component/common/buttons/ChipButton.tsx',
    area: 'Import',
    kind: 'Chip',
    structural: ['background-color', 'border', 'border-radius'],
    decls: [
      { prop: 'border', palette: 'emphasis.200', prefix: '1px solid' },
      { prop: 'border-radius', value: '50px' },
      { prop: 'padding', spacing: [0.125, 1.5] },
      { prop: 'background-color', palette: 'background.default', prefix: '' },
      { prop: 'cursor', value: 'pointer' },
      { prop: 'min-width', value: '0px' },
    ],
    users: 1,
  },
  {
    component: 'StyledFlagButton',
    name: 'FlagSelectorContent',
    file: 'component/languages/FlagSelector/FlagSelectorContent.tsx',
    area: 'Language flag picker',
    kind: 'Icon button',
    structural: ['height'],
    decls: [
      { prop: 'padding', spacing: [0.5] },
      { prop: 'min-width', value: '0px' },
      { prop: 'height', value: '29px' },
    ],
    users: 1,
  },
  {
    component: 'StyledExitDebugButton',
    name: 'DebuggingCustomerAccountAnnouncement',
    file: 'component/layout/TopBar/announcements/DebuggingCustomerAccountAnnouncement.tsx',
    area: 'Top bar',
    kind: 'Banner action',
    structural: ['border-color', 'color'],
    decls: [
      { prop: 'color', value: 'inherit' },
      { prop: 'border-color', value: 'rgba(135, 135, 135, 0.38)' },
    ],
    users: 1,
  },
  {
    component: 'StyledButton',
    name: 'LanguagePermissionsMenu',
    file: 'component/security/LanguagePermissionsMenu.tsx',
    area: 'Permissions',
    kind: 'Menu trigger',
    structural: ['background'],
    decls: [
      { prop: 'padding', value: '0 5px 0 7px' },
      { prop: 'background', palette: 'background.default', prefix: '' },
    ],
    users: 3,
  },
  {
    component: 'StyledToggleButton',
    name: 'TranslationControlsCompact',
    file: 'views/projects/translations/TranslationHeader/TranslationControlsCompact.tsx',
    area: 'Translations header',
    kind: 'Toggle',
    structural: ['height', 'min-height', 'width'],
    decls: [
      { prop: 'padding', value: '0px 2px' },
      { prop: 'height', value: '35px' },
      { prop: 'min-height', value: '35px' },
      { prop: 'width', value: '22px' },
    ],
    users: 1,
  },
];
