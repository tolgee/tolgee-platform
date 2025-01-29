import { useEffect, useState } from 'react';

export function useWindowInnerHeight() {
  const [height, setHeight] = useState<number>();

  useEffect(() => {
    const handleResize = () => {
      setHeight(window.innerHeight);
    };

    handleResize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return height || 0;
}
