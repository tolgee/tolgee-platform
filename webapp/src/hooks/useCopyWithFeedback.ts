import { useEffect, useRef, useState } from 'react';
import copy from 'copy-to-clipboard';

const COPIED_FEEDBACK_MS = 2000;

export const useCopyWithFeedback = () => {
  const [copied, setCopied] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => () => clearTimeout(timerRef.current), []);

  const copyText = (text: string) => {
    if (!copy(text)) {
      return;
    }
    setCopied(true);
    clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => setCopied(false), COPIED_FEEDBACK_MS);
  };

  return { copied, copyText };
};
