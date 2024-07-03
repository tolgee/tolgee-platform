import { Mt, TranslationMemory as Tm } from 'tg.component/CustomIcons';
import { MachineTranslation } from './panels/MachineTranslation/MachineTranslation';
import { T } from '@tolgee/react';
import { TranslationMemory } from './panels/TranslationMemory/TranslationMemory';
import { Comments, commentsCount } from './panels/Comments/Comments';
import { History } from './panels/History/History';
import {
  Keyboard02,
  ClockRewind,
  ClipboardCheck,
  MessageTextSquare02,
} from '@untitled-ui/icons-react';
import { PanelConfig } from './common/types';
import { KeyboardShortcuts } from './panels/KeyboardShortcuts/KeyboardShortcuts';
import { Tasks, tasksCount } from './panels/Tasks/Tasks';

export const PANELS_WHEN_INACTIVE = [
  {
    id: 'keyboard_shortcuts',
    icon: <Keyboard02 />,
    name: <T keyName="translation_tools_keyboard_shortcuts" />,
    component: KeyboardShortcuts,
  },
];

export const PANELS = [
  {
    id: 'machine_translation',
    icon: <Mt />,
    name: <T keyName="translation_tools_machine_translation" />,
    component: MachineTranslation,
    displayPanel: ({ language, editEnabled }) => !language.base && editEnabled,
  },
  {
    id: 'translation_memory',
    icon: <Tm />,
    name: <T keyName="translation_tools_translation_memory" />,
    component: TranslationMemory,
    displayPanel: ({ language, editEnabled }) => !language.base && editEnabled,
  },
  {
    id: 'comments',
    icon: <MessageTextSquare02 />,
    name: <T keyName="translation_tools_comments" />,
    component: Comments,
    itemsCountFunction: commentsCount,
  },
  {
    id: 'history',
    icon: <ClockRewind />,
    name: <T keyName="translation_tools_history" />,
    component: History,
  },
  {
    id: 'tasks',
    icon: <ClipboardCheck />,
    name: <T keyName="translation_tools_tasks" />,
    component: Tasks,
    itemsCountFunction: tasksCount,
    displayPanel: ({ projectPermissions }) =>
      projectPermissions.satisfiesPermission('tasks.view'),
    hideWhenCountZero: true,
    hideCount: true,
  },
] satisfies PanelConfig[];
