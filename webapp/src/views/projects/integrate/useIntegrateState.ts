import { useProject } from 'tg.hooks/useProject';
import { guides } from 'tg.views/projects/integrate/guides';
import { Guide } from 'tg.views/projects/integrate/types';
import { components } from 'tg.service/apiSchema.generated';
import { useEffect, useState } from 'react';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import * as Sentry from '@sentry/browser';

const LOCALSTORAGE_MEMORY_NAME = 'tolgee_integrate_memory';

type MemoryItem = {
  weaponName?: string;
  apiKeyId?: number;
};

type Memory = Record<number, MemoryItem>;

export const useIntegrateState = () => {
  const project = useProject();

  const [newKey, setNewKey] = useState(
    undefined as components['schemas']['ApiKeyModel'] | undefined
  );

  const getStoredMemory = () => {
    const raw = localStorage.getItem(LOCALSTORAGE_MEMORY_NAME);
    try {
      return raw ? (JSON.parse(raw) as Memory) : undefined;
    } catch (e) {
      Sentry.captureException(e);
      return undefined;
    }
  };

  const getMemoryItem = (projectId: number): MemoryItem | undefined => {
    const memory = getStoredMemory() || {};
    return memory[projectId];
  };

  const stored = getMemoryItem(project.id);

  const getDefaultWeapon = () =>
    stored
      ? guides.find((w) => w.name === stored.weaponName)
      : (undefined as Guide | undefined);

  const getDefaultApiKey = () =>
    keys?.find((k) => k.id === stored?.apiKeyId) as
      | components['schemas']['ApiKeyModel']
      | undefined;

  const [selectedWeapon, setSelectedWeapon] = useState(getDefaultWeapon());

  const keysLoadable = useApiQuery({
    url: '/v2/api-keys',
    method: 'get',
    query: {
      filterProjectId: project.id,
      pageable: {
        size: 1000,
      },
    },
  });

  const keys = keysLoadable.data?._embedded?.apiKeys;

  useEffect(() => {
    setSelectedApiKey(getDefaultApiKey());
  }, [keys]);

  const [selectedApiKey, setSelectedApiKey] = useState(
    keys?.find((k) => k.id === stored?.apiKeyId) as
      | components['schemas']['ApiKeyModel']
      | components['schemas']['RevealedApiKeyModel']
      | undefined
  );

  const onWeaponSelect = (guide: Guide) => {
    storeMemoryItem(project.id, {
      weaponName: guide.name,
      apiKeyId: selectedApiKey?.id,
    });
    setSelectedWeapon(guide);
  };

  const storeMemoryItem = (projectId: number, item: MemoryItem) => {
    let memory = getStoredMemory() || {};
    // Don't let grow memory forever.
    // Data in the memory are not important, so
    // there is no need to implement any smart
    // logic for this.
    // When there is more then 100 items,
    // it's deleted all
    if (Object.keys(memory).length > 100) {
      memory = {};
    }
    memory[projectId] = item;
    localStorage.setItem(LOCALSTORAGE_MEMORY_NAME, JSON.stringify(memory));
  };

  const onSelectApiKey = (apiKey: components['schemas']['ApiKeyModel']) => {
    storeMemoryItem(project.id, {
      weaponName: selectedWeapon?.name,
      apiKeyId: apiKey.id,
    });
    setSelectedApiKey(apiKey);
  };

  useEffect(() => {
    if (keys && newKey) {
      if (keys.findIndex((k) => k.id === newKey.id) > -1) {
        onSelectApiKey(newKey);
        setNewKey(undefined);
      }
    }
  }, [keys, newKey]);

  return {
    onWeaponSelect,
    onSelectApiKey,
    selectedWeapon,
    selectedApiKey,
    keys,
    keysLoading: keysLoadable.isLoading,
    onNewKeyCreated: (key: components['schemas']['ApiKeyModel']) => {
      keysLoadable.refetch();
      setNewKey(key);
    },
  };
};
