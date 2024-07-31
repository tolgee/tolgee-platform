import {
  MachineTranslationIcon,
  TranslationMemoryIcon,
} from 'tg.component/CustomIcons';
import { MachineTranslation } from './panels/MachineTranslation/MachineTranslation';
import { T } from '@tolgee/react';
import { TranslationMemory } from './panels/TranslationMemory/TranslationMemory';
import { Comments, CommentsItemsCount } from './panels/Comments/Comments';
import { History } from './panels/History/History';
import {
  Message,
  History as HistoryIcon,
  Keyboard,
  Task,
} from '@mui/icons-material';
import { PanelConfig } from './common/types';
import { KeyboardShortcuts } from './panels/KeyboardShortcuts/KeyboardShortcuts';
import { Tasks } from './panels/Tasks/Tasks';

export const PANELS_WHEN_INACTIVE = [
  {
    id: 'keyboard_shortcuts',
    icon: <Keyboard fontSize="small" />,
    name: <T keyName="translation_tools_keyboard_shortcuts" />,
    component: KeyboardShortcuts,
  },
];

export const PANELS = [
  {
    id: 'machine_translation',
    icon: <MachineTranslationIcon fontSize="small" />,
    name: <T keyName="translation_tools_machine_translation" />,
    component: MachineTranslation,
    displayPanel: ({ language, editEnabled }) => !language.base && editEnabled,
  },
  {
    id: 'translation_memory',
    icon: <TranslationMemoryIcon fontSize="small" />,
    name: <T keyName="translation_tools_translation_memory" />,
    component: TranslationMemory,
    displayPanel: ({ language, editEnabled }) => !language.base && editEnabled,
  },
  {
    id: 'comments',
    icon: <Message fontSize="small" />,
    name: <T keyName="translation_tools_comments" />,
    component: Comments,
    itemsCountComponent: CommentsItemsCount,
  },
  {
    id: 'history',
    icon: <HistoryIcon fontSize="small" />,
    name: <T keyName="translation_tools_history" />,
    component: History,
  },
  {
    id: 'tasks',
    icon: <Task fontSize="small" />,
    name: <T keyName="translation_tools_tasks" />,
    component: Tasks,
  },
] satisfies PanelConfig[];
