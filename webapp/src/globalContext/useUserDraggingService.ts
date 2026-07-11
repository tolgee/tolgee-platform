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

    document.body.addEventListener('dragenter', dragStartHandler, true);
    document.body.addEventListener('dragover', dragStartHandler, true);
    document.body.addEventListener('dragleave', dragEndHandler, true);
    document.body.addEventListener('drop', dragEndHandler, true);
    document.body.addEventListener('click', dragEndHandler, true);
    document.addEventListener('visibilitychange', dragEndHandler);
    return () => {
      document.body.removeEventListener('dragenter', dragStartHandler, {
        capture: true,
      });
      document.body.removeEventListener('dragover', dragStartHandler, true);
      document.body.removeEventListener('dragleave', dragEndHandler, true);
      document.body.removeEventListener('drop', dragEndHandler, true);
      document.body.removeEventListener('click', dragEndHandler, true);
      document.removeEventListener('visibilitychange', dragEndHandler);
      clearTimeout(timeout);
    };
  }, []);

  return { userIsDragging };
};
