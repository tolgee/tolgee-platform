import { useCallback, useMemo } from 'react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import {
  substituteUrlTemplate,
  UrlTemplateVars,
} from '../translations/decorators/substituteUrlTemplate';
import { useAppModal } from './AppModalContext';

type ProjectAppModel = components['schemas']['ProjectAppModel'];
type ModalModule = components['schemas']['ModalModule'];

/**
 * Module types whose entries are exposed via {@link useAppTriggers}. Every
 * trigger module shares the same shape: { key, title, icon?, type,
 * urlTemplate?, modalKey? } (plus `combination` for shortcuts).
 */
export type TriggerModuleKey =
  | 'bulk-action'
  | 'translations-toolbar-action'
  | 'project-menu-action'
  | 'shortcut';

type AnyTriggerItem = {
  key: string;
  title?: string;
  icon?: string;
  type: 'link' | 'panel' | 'tab' | 'modal';
  urlTemplate?: string;
  modalKey?: string;
  combination?: string;
};

export type ResolvedTrigger = {
  install: ProjectAppModel;
  item: AnyTriggerItem;
};

/**
 * Returns every enabled-app's trigger entries for a given anchor, paired
 * with the owning install (needed to mint tokens and resolve modalKey).
 */
export function useAppTriggers(
  projectId: number,
  anchor: TriggerModuleKey
): ResolvedTrigger[] {
  const apps = useApiQuery({
    url: '/v2/projects/{projectId}/apps',
    method: 'get',
    path: { projectId },
    options: { staleTime: 60_000 },
  });

  return useMemo(() => {
    const installs = apps.data?._embedded?.projectApps ?? [];
    const out: ResolvedTrigger[] = [];
    for (const install of installs) {
      if (!install.enabled) continue;
      const list = (
        install.modules as Record<string, AnyTriggerItem[] | undefined>
      )?.[anchor];
      if (!list) continue;
      for (const item of list) out.push({ install, item });
    }
    return out;
  }, [apps.data, anchor]);
}

export type DispatchOptions = {
  templateVars?: UrlTemplateVars;
  /** Anchor-specific fields merged into the modal iframe's init payload. */
  extraInitPayload?: Record<string, unknown>;
  keyId?: number | null;
  languageId?: number | null;
  languageTag?: string | null;
  translationId?: number | null;
};

/**
 * Returns a dispatch function that maps a trigger item (link or modal) to
 * the appropriate side effect — opening a URL or the shared app modal.
 */
export function useAppTriggerDispatch(): (
  trigger: ResolvedTrigger,
  options?: DispatchOptions
) => void {
  const modal = useAppModal();
  return useCallback(
    (trigger, options) => {
      const { install, item } = trigger;
      if (item.type === 'link') {
        const template = item.urlTemplate;
        if (!template) return;
        const url = substituteUrlTemplate(
          template,
          options?.templateVars ?? {}
        );
        window.open(url, '_blank', 'noopener,noreferrer');
        return;
      }
      if (item.type === 'modal') {
        if (!item.modalKey) return;
        const modalDef = lookupModal(install, item.modalKey);
        if (!modalDef) return;
        modal.open({
          installId: install.id,
          baseUrl: install.baseUrl,
          entry: modalDef.entry,
          title: modalDef.title,
          icon: modalDef.icon ?? item.icon,
          width: modalDef.width,
          height: modalDef.height,
          extraInitPayload: options?.extraInitPayload,
          keyId: options?.keyId,
          languageId: options?.languageId,
          languageTag: options?.languageTag,
          translationId: options?.translationId,
        });
      }
    },
    [modal]
  );
}

const lookupModal = (
  install: ProjectAppModel,
  modalKey: string
): ModalModule | null => {
  const modals = install.modules?.['modal'] ?? [];
  return modals.find((m) => m.key === modalKey) ?? null;
};
