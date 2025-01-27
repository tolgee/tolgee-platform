import { useEffect, useState } from 'react';

type Size = {
  width: number;
  height: number;
};

type Props = {
  src: string;
  enabled?: boolean;
};

export const useImagePreload = ({ src, enabled = true }: Props) => {
  const [size, setSize] = useState({ width: 0, height: 0 });
  const [isLoading, setIsLoading] = useState(false);
  useEffect(() => {
    if (enabled) {
      setIsLoading(true);
      let mounted = true;
      const img = new Image();
      img.src = src;
      img.onload = () => {
        if (mounted) {
          setSize({ width: img.naturalWidth, height: img.naturalHeight });
          setIsLoading(false);
        }
      };
      return () => {
        mounted = false;
      };
    } else {
      setIsLoading(false);
      setSize({ width: 0, height: 0 });
    }
  }, [src]);

  return { size, isLoading };
};

export function scaleImage(imageSize: Size, containerSize: Size): Size {
  if (
    imageSize.width < containerSize.width &&
    imageSize.height < containerSize.height
  ) {
    return imageSize;
  }
  const imageRatio = imageSize.width / imageSize.height;
  const containerRatio = containerSize.width / containerSize.height;
  let scaledSize: Size;
  if (imageRatio > containerRatio) {
    scaledSize = {
      width: containerSize.width,
      height: containerSize.width / imageRatio,
    };
  } else {
    scaledSize = {
      height: containerSize.height,
      width: containerSize.height * imageRatio,
    };
  }
  return scaledSize;
}
