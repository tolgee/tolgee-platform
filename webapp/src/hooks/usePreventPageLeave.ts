import { useEffect } from 'react';

export const usePreventPageLeave = (active: boolean) => {
  useEffect(() => {
    // prevent leaving the page when there are unsaved changes
    if (active) {
      const handler = (e) => {
        e.preventDefault();
        e.returnValue = '';
      };
      window.addEventListener('beforeunload', handler);
      return () => window.removeEventListener('beforeunload', handler);
    }
  }, [active]);
};
