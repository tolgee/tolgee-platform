import React from 'react';
import {
  CheckVerified01,
  ClipboardCheck,
  Cloud01,
  Edit02,
  File06,
  GitBranch02,
  Globe01,
  Image01,
  Key01,
  LayersTwo01,
  MessageTextSquare02,
  Package,
  Plus,
  Settings02,
  Tag01,
  Translate01,
  Trash01,
  UploadCloud01,
  Zap,
} from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';

type ActivityGroupTypeEnum =
  components['schemas']['ActivityGroupModel']['type'];

export type GroupSemantic = 'create' | 'delete' | 'neutral';

const DOMAIN_ICONS: [string, React.ComponentType<any>][] = [
  ['SCREENSHOT', Image01],
  ['COMMENT', MessageTextSquare02],
  ['TAG', Tag01],
  ['CHARACTER_LIMIT', Key01],
  ['KEY', Key01],
  ['BASE_TRANSLATION', Translate01],
  ['TRANSLATION_MEMORY', LayersTwo01],
  ['TRANSLATION_LABEL', Tag01],
  ['LABEL', Tag01],
  ['REVIEW', CheckVerified01],
  ['SUGGESTION', MessageTextSquare02],
  ['TRANSLATION', Translate01],
  ['LANGUAGE', Globe01],
  ['NAMESPACE', Package],
  ['PROJECT', Settings02],
  ['IMPORT', UploadCloud01],
  ['GLOSSARY', File06],
  ['TASK', ClipboardCheck],
  ['BRANCH', GitBranch02],
  ['AI_PROMPT', Zap],
  ['QA_ISSUE', CheckVerified01],
  ['WEBHOOK', Zap],
  ['CONTENT_DELIVERY', Cloud01],
  ['CONTENT_STORAGE', Cloud01],
  ['OUTDATED', Translate01],
  ['AUTO_TRANSLATE', Translate01],
];

export function getGroupIcon(
  type: ActivityGroupTypeEnum
): React.ComponentType<any> {
  const domainIcon = DOMAIN_ICONS.find(([domain]) =>
    type.includes(domain)
  )?.[1];
  if (domainIcon) {
    return domainIcon;
  }
  if (type.startsWith('CREATE') || type.startsWith('ADD')) {
    return Plus;
  }
  if (type.includes('DELETE')) {
    return Trash01;
  }
  return Edit02;
}

export function getGroupSemantic(type: ActivityGroupTypeEnum): GroupSemantic {
  if (
    type.startsWith('CREATE') ||
    type.startsWith('ADD') ||
    type.startsWith('IMPORT') ||
    type === 'RESTORE_KEY'
  ) {
    return 'create';
  }
  if (type.includes('DELETE')) {
    return 'delete';
  }
  return 'neutral';
}
