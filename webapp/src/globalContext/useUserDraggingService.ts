import { useEffect, useState } from 'react';

export const useUserDraggingService = () => {
  const [userIsDragging, setUserIsDragging] = useState(false);

  useEffect(() => {
    let showDrag = false;
    let timeout: ReturnType<typeof setTimeout> | undefined = undefined;
    const dragStartHandler = () => {
      showDrag = true;
      setUserIsDragging(true);
    };
    const dragEndHandler = () => {
      showDrag = false;
      clearTimeout(timeout);
      timeout = setTimeout(function () {
        if (!showDrag) {
          setUserIsDragging(false);
        }
      }, 200);
    };

    const root = document.getElementById('root')!;

    root.addEventListener('dragenter', dragStartHandler, true);
    root.addEventListener('dragover', dragStartHandler, true);
    root.addEventListener('dragleave', dragEndHandler, true);
    root.addEventListener('drop', dragEndHandler, true);
    return () => {
      root.removeEventListener('dragenter', dragStartHandler, {
        capture: true,
      });
      root.removeEventListener('dragover', dragStartHandler, true);
      root.removeEventListener('dragleave', dragEndHandler, true);
      root.removeEventListener('drop', dragEndHandler, true);
      clearTimeout(timeout);
    };
  }, []);

  return { userIsDragging };
};
