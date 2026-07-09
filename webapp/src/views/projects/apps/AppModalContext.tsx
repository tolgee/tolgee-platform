import {
  createContext,
  ReactNode,
  useCallback,
  useContext,
  useMemo,
  useState,
} from 'react';

import { AppModalDialog, AppModalRequest } from './AppModalDialog';

type AppModalContextValue = {
  open: (request: AppModalRequest) => void;
  close: () => void;
};

const AppModalContext = createContext<AppModalContextValue | null>(null);

/**
 * Hosts a single, application-wide app modal slot. Plugins trigger it via
 * useAppModal().open(); subsequent open() calls replace the current modal.
 * Mounted by a layout that wraps both the project sidebar and the routed
 * content so any trigger surface can access it.
 */
export const AppModalProvider = ({ children }: { children: ReactNode }) => {
  const [request, setRequest] = useState<AppModalRequest | null>(null);

  const open = useCallback((req: AppModalRequest) => setRequest(req), []);
  const close = useCallback(() => setRequest(null), []);

  const value = useMemo(() => ({ open, close }), [open, close]);

  return (
    <AppModalContext.Provider value={value}>
      {children}
      {request && <AppModalDialog request={request} onClose={close} />}
    </AppModalContext.Provider>
  );
};

export const useAppModal = (): AppModalContextValue => {
  const ctx = useContext(AppModalContext);
  if (!ctx) {
    throw new Error('useAppModal must be used inside AppModalProvider');
  }
  return ctx;
};
