import * as untitled from '../../src/icons';
import * as custom from '../../src/icons/custom';

/**
 * What each icon means in Tolgee — not what it depicts. The glyph is generic; the meaning is ours,
 * and it is the thing a name like `Shield01` or `Zap` cannot carry.
 *
 * `prefer` marks a glyph that overlaps with another one already in use. Both are listed because
 * both are in the code; the page marks the overlap so it can be settled rather than grown.
 *
 * Meanings are keyed by source because the two sets collide: `ArrowRight` exists in both.
 */
export type IconMeaning = {
  means: string;
  prefer?: string;
  /**
   * Nothing in the webapp or billing imports it. A snapshot taken 2026-08-17, not a live check —
   * that would mean this package reading its consumers' source. Refresh it by hand when in doubt.
   */
  unused?: true;
};

export type IconSource = 'untitled' | 'custom';

type IconComponent = (props: {
  width?: number;
  height?: number;
}) => JSX.Element;

const byName = (a: string, b: string) =>
  a.localeCompare(b, 'en', { numeric: true, sensitivity: 'base' });

export const iconEntries = (source: IconSource) =>
  Object.entries(
    (source === 'untitled' ? untitled : custom) as Record<
      string,
      IconComponent
    >,
  ).sort(([a], [b]) => byName(a, b));

/** Anchors are shared by the grid and the meanings list, so a click lands on the right entry. */
export const anchorOf = (source: IconSource, name: string) =>
  `icon-${source}-${name.toLowerCase()}`;

export const ICON_MEANINGS: Record<IconSource, Record<string, IconMeaning>> = {
  untitled: {
    // Actions
    XClose: {
      means: 'Close — dismisses a dialog, panel or banner, removes a chip.',
    },
    X: { means: 'Close, lighter weight.', prefer: 'XClose' },
    Plus: {
      means: 'Add — the primary create action on lists and detail screens.',
    },
    PlusCircle: {
      means:
        'Add, emphasized — empty states and stat blocks where a plain Plus would be lost.',
    },
    MinusCircle: {
      means: 'Remove — the counterpart of PlusCircle in merges and stats.',
    },
    Trash01: { means: 'Delete permanently.' },
    Edit02: { means: 'Edit — switches an item into edit mode.' },
    Edit01: { means: 'Edit, alternative weight.', prefer: 'Edit02' },
    Edit05: { means: 'Contribute a translation, from the community banner.' },
    Copy06: { means: 'Copy to clipboard.' },
    RefreshCcw01: { means: 'Refresh — reloads the data on screen.' },
    RefreshCcw02: { means: 'Recalculate statistics.', prefer: 'RefreshCcw01' },
    ReverseLeft: { means: 'Revert a suggestion back to the previous value.' },
    PlayCircle: { means: 'Run a batch operation.' },
    Send03: { means: 'Send a comment.' },
    Share03: { means: 'Share an activity item.' },
    CameraPlus: { means: 'Attach a screenshot to a key.' },

    // Navigation and layout
    ChevronDown: { means: 'Expand a collapsed section.' },
    ChevronUp: { means: 'Collapse an expanded section.' },
    ChevronRight: {
      means: 'Forward — pagination, scrolling, drilling into a detail.',
    },
    ChevronLeft: { means: 'Back.' },
    DotsVertical: { means: 'Row menu — the context menu of a single item.' },
    DotsHorizontal: { means: 'More — collapsed QA badges.' },
    DotsGrid: { means: 'Drag handle for reordering.' },
    HomeLine: { means: 'Home — my tasks, project dashboard.' },
    LayoutGrid02: { means: 'Grid view of translations.' },
    LayoutLeft: { means: 'List view of translations.' },
    LayoutAlt04: { means: 'Project overview.' },
    Rows03: { means: 'Task list as rows.' },
    SearchSm: { means: 'Search — marks a search input.' },
    FilterLines: { means: 'Filter.' },
    Folder: { means: 'A project used as a source to copy from.' },

    // Status
    Check: {
      means: 'Done — a resolved comment, a passed check, a completed step.',
    },
    CheckCircle: {
      means: 'Success — a finished operation or a satisfied condition.',
    },
    CheckCircleBroken: { means: 'Partially done.' },
    CheckDone01: {
      means:
        'Settled — an import conflict or suggestion that has been resolved.',
    },
    XCircle: { means: 'Remove a file that was dropped in.' },
    AlertTriangle: { means: 'Warning — needs attention but is not a failure.' },
    AlertCircle: { means: 'Error — a failed webhook or import row.' },
    InfoCircle: { means: 'Neutral explanation of a setting or a price.' },
    HelpCircle: { means: 'Hint — opens help next to a field or setting.' },
    Lightbulb02: { means: 'Example — a preview dataset.' },
    Flag02: { means: 'Translation state marker.' },
    Zap: { means: 'Machine translated — marks an auto-filled key cell.' },

    // Domain
    Translate01: { means: 'Translating as an activity, not a language.' },
    Globe01: { means: 'The languages of a project.' },
    Globe02: { means: 'Language switch in the translation controls.' },
    BookClosed: { means: 'Glossary.' },
    BookOpen01: { means: 'Documentation.' },
    ClipboardCheck: {
      means: 'Task — a unit of work assigned to a translator or reviewer.',
    },
    AlarmClock: { means: 'Task due date.' },
    BarChartSquare01: { means: 'Task statistics.' },
    Calendar: { means: 'Date picker.' },
    ClockRewind: { means: 'History — the activity log.' },
    ClockStopwatch: { means: 'Trial countdown.' },
    ShoppingCart01: { means: 'Ordering — paid translations and paid seats.' },
    MessageTextSquare02: { means: 'Comment or discussion on a key.' },
    MessageSquare01: { means: 'Chat support.' },
    Users01: { means: 'Community translators.' },
    User01: { means: 'A single user or member.' },
    Bell01: { means: 'Notifications.' },
    Mail01: { means: 'E-mail — contact and invitations.' },
    Link02: { means: 'Invitation link.' },
    ArrowNarrowRight: { means: 'Source term to target term in the glossary.' },
    ArrowUpRight: { means: 'External link — leaves Tolgee.' },
    LinkExternal01: { means: 'External link.', prefer: 'ArrowUpRight' },
    LinkExternal02: { means: 'External link.', prefer: 'ArrowUpRight' },
    SwitchHorizontal01: {
      means: 'A connection between Tolgee and another system.',
    },

    // Files
    File02: { means: 'A selected or droppable file.' },
    UploadCloud02: { means: 'Import — brings data in from a file.' },
    UploadCloud01: {
      means: 'Drop area for an upload.',
      prefer: 'UploadCloud02',
    },
    FileDownload03: { means: 'Export — takes data out to a file.' },

    // Development and integration
    Code01: { means: 'Integration snippet in a guide.' },
    Code02: {
      means: 'Generated code — an AI result or the project code view.',
    },
    Terminal: { means: 'CLI guide.' },
    GitBranch02: { means: 'A branch.' },
    GitMerge: { means: 'Merging a branch.' },
    ShieldTick: { means: 'A protected branch.' },

    // Account and appearance
    Key02: { means: 'Credentials — password and SSO sign-in.' },
    LogIn01: { means: 'Sign in through a third party.' },
    Eye: { means: 'Reveal the password.' },
    EyeOff: { means: 'Hide the password.' },
    Settings01: { means: 'Settings of the thing at hand.' },
    Moon01: { means: 'Dark theme.' },
    Sun: { means: 'Light theme.' },

    // Keyboard hints — rendered inside shortcut chips, not as UI actions
    ArrowUp: {
      means: 'The up arrow key, drawn inside a keyboard shortcut hint.',
    },
    ArrowDown: {
      means: 'The down arrow key, drawn inside a keyboard shortcut hint.',
    },
    ArrowLeft: {
      means: 'The left arrow key, drawn inside a keyboard shortcut hint.',
    },
    ArrowRight: {
      means: 'The right arrow key, drawn inside a keyboard shortcut hint.',
    },
    CornerDownLeft: {
      means: 'The Enter key, drawn inside a keyboard shortcut hint.',
    },
    Keyboard02: { means: 'Keyboard shortcuts.' },
  },

  custom: {
    // Brand marks — the one group with no Untitled UI equivalent
    GitHub: { means: 'GitHub, on sign-in and in the help menu.' },
    Google: { means: 'Google, on sign-in.' },
    Slack: { means: 'The Slack integration.' },

    // Tolgee concepts the icon set has no glyph for
    Mt: { means: 'Machine translation, as a source of a translation.' },
    MachineTranslation: {
      means: 'Machine translation, second drawing.',
      prefer: 'Mt',
      unused: true,
    },
    TranslationMemory: { means: 'Translation memory as a source.' },
    TranslationMemoryAlt: {
      means:
        'Translation memory, an older simpler drawing that nothing imports.',
      prefer: 'TranslationMemory',
      unused: true,
    },
    BackTranslation: { means: 'Back translation of a segment.', unused: true },
    Suggestion: { means: 'A suggested translation.', unused: true },
    OtherLanguages: {
      means: 'The remaining languages of a key.',
      unused: true,
    },
    Translation: { means: 'A translation as an object.', unused: true },
    QaCheck: { means: 'A quality-assurance check.' },
    CheckCircleDash: { means: 'A check that has not run yet.' },
    QsFinished: { means: 'The quick start guide, finished.' },
    Taskinfo: { means: 'Details of a task.', unused: true },
    TaskDetail: {
      means: 'Task detail panel.',
      prefer: 'Taskinfo',
      unused: true,
    },
    Projects: { means: 'The project list.', unused: true },
    Integration: { means: 'An integration with another tool.' },
    Dropzone: { means: 'Drop area art for file upload.', unused: true },
    Preview: { means: 'Preview of a dataset or result.', unused: true },
    Stars: { means: 'AI — marks a generated suggestion.' },
    CopyBase: { means: 'Copy the base language value into a translation.' },

    // Celebration art
    Tada: { means: 'Celebration after finishing a flow.' },
    Celebration: {
      means: 'Celebration, second drawing.',
      prefer: 'Tada',
      unused: true,
    },
    Rocket: { means: 'Launch or upgrade.', unused: true },
    RocketFilled: { means: 'Launch or upgrade, filled.', prefer: 'Rocket' },
    CameraSad: {
      means: 'A screenshot that could not be loaded.',
      unused: true,
    },

    // Overlapping with the Untitled UI set — kept because they are still imported
    ArrowRight: {
      means: 'Right arrow. Collides by name with the Untitled UI one.',
      prefer: 'ArrowRight from the Untitled UI set',
    },
    ArrowDropDown: { means: 'Dropdown caret.', prefer: 'ChevronDown' },
    Branch: { means: 'A branch.', prefer: 'GitBranch02' },
    CheckDone: { means: 'Completed.', prefer: 'CheckDone01' },
    Settings: { means: 'Settings.', prefer: 'Settings01', unused: true },
    Sort: { means: 'Sorting of a list.' },
    FilterLines2: {
      means: 'Filter, second drawing.',
      prefer: 'FilterLines',
      unused: true,
    },
    Import: { means: 'Import.', prefer: 'UploadCloud02', unused: true },
    Export: { means: 'Export.', prefer: 'FileDownload03', unused: true },
    UserAdd: { means: 'Invite a user.', unused: true },
    UserSetting: { means: 'User settings.', unused: true },
    CheckBoxOutlineBlank: {
      means: 'An empty checkbox, left over from the Material icon set.',
    },
    TwoLinesVertical: { means: 'A two-line layout marker.', unused: true },
  },
};
