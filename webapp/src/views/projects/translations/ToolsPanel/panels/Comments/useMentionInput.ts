import React, { useEffect, useRef, useState } from 'react';
import { useDebounce } from 'use-debounce';

import { components } from 'tg.service/apiSchema.generated';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import {
  MentionTarget,
  mentionsToTokens,
} from 'tg.component/mentions/mentionTokens';

type MentionedUserModel = components['schemas']['MentionedUserModel'];
type LanguageModel = components['schemas']['LanguageModel'];

type Trigger = { start: number; query: string };

const TRIGGER_REGEX = /(^|\s)@([^\s@]{0,40})$/;
const PUNCTUATION_REGEX = /^[.,!?;:)]$/;

type Props = {
  keyId: number;
  language: LanguageModel;
  value: string;
  setValue: (value: string) => void;
};

export const useMentionInput = ({
  keyId,
  language,
  value,
  setValue,
}: Props) => {
  const project = useProject();
  const inputRef = useRef<HTMLTextAreaElement | HTMLInputElement | null>(null);
  const [trigger, setTrigger] = useState<Trigger | undefined>();
  const [dismissedStart, setDismissedStart] = useState<number | undefined>();
  const [highlighted, setHighlighted] = useState(0);
  const [mentioned, setMentioned] = useState<MentionTarget[]>([]);
  const lastPick = useRef<{ start: number; spaceAt: number } | undefined>();

  const [debouncedQuery] = useDebounce(trigger?.query ?? '', 200);

  const usersLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/mentionable-users',
    method: 'get',
    path: { projectId: project.id },
    query: {
      keyId,
      languageId: language.id,
      search: debouncedQuery || undefined,
      size: 8,
    },
    options: {
      enabled: Boolean(trigger),
      keepPreviousData: true,
    },
  });

  const users: MentionedUserModel[] =
    usersLoadable.data?._embedded?.users ?? [];

  useEffect(() => {
    setHighlighted(0);
  }, [usersLoadable.data]);

  const updateTrigger = (text: string, caret: number | null) => {
    if (caret === null) {
      setTrigger(undefined);
      return;
    }
    const match = TRIGGER_REGEX.exec(text.slice(0, caret));
    if (!match) {
      setTrigger(undefined);
      setDismissedStart(undefined);
      return;
    }
    const start = caret - match[2].length - 1;
    if (start === dismissedStart) {
      setTrigger(undefined);
      return;
    }
    setTrigger({ start, query: match[2] });
  };

  const placeCaret = (caret: number) => {
    setTimeout(() => {
      inputRef.current?.focus();
      inputRef.current?.setSelectionRange(caret, caret);
    });
  };

  const dropSpaceBeforePunctuation = (typed: string, caret: number | null) => {
    const spaceAt = lastPick.current?.spaceAt;
    const typedRightAfterSpace =
      spaceAt !== undefined &&
      caret === spaceAt + 2 &&
      typed.length === value.length + 1 &&
      typed[spaceAt] === ' ' &&
      PUNCTUATION_REGEX.test(typed[spaceAt + 1]);
    if (!typedRightAfterSpace) {
      return undefined;
    }
    return {
      text: typed.slice(0, spaceAt) + typed.slice(spaceAt + 1),
      caret: spaceAt + 1,
    };
  };

  const handleChange = (
    e: React.ChangeEvent<HTMLTextAreaElement | HTMLInputElement>
  ) => {
    const typed = e.target.value;
    const caret = e.target.selectionStart;
    const withoutSpace = dropSpaceBeforePunctuation(typed, caret);
    const pickStart = lastPick.current?.start;
    lastPick.current = undefined;
    if (withoutSpace) {
      setDismissedStart(pickStart);
      setTrigger(undefined);
      setValue(withoutSpace.text);
      placeCaret(withoutSpace.caret);
      return;
    }
    setValue(typed);
    updateTrigger(typed, caret);
  };

  const handleSelect = (
    e: React.SyntheticEvent<HTMLTextAreaElement | HTMLInputElement>
  ) => {
    const target = e.currentTarget;
    updateTrigger(target.value, target.selectionStart);
  };

  const close = () => {
    setTrigger(undefined);
  };

  const selectUser = (user: MentionedUserModel) => {
    if (!trigger) {
      return;
    }
    const caret = inputRef.current?.selectionStart ?? value.length;
    const inserted = `@${user.name} `;
    const newValue =
      value.slice(0, trigger.start) + inserted + value.slice(caret);
    const newCaret = trigger.start + inserted.length;
    setValue(newValue);
    setMentioned((prev) => [...prev, { id: user.id, name: user.name }]);
    setTrigger(undefined);
    lastPick.current = { start: trigger.start, spaceAt: newCaret - 1 };
    placeCaret(newCaret);
  };

  const pickerOpen = Boolean(trigger);

  const consumePickerKey = (e: React.KeyboardEvent): boolean => {
    if (!pickerOpen) {
      return false;
    }
    if (e.key === 'Escape') {
      e.preventDefault();
      e.stopPropagation();
      setDismissedStart(trigger?.start);
      close();
      return true;
    }
    if (!users.length) {
      return false;
    }
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setHighlighted((i) => (i + 1) % users.length);
      return true;
    }
    if (e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlighted((i) => (i - 1 + users.length) % users.length);
      return true;
    }
    if (e.key === 'Enter' || e.key === 'Tab') {
      e.preventDefault();
      e.stopPropagation();
      selectUser(users[Math.min(highlighted, users.length - 1)]);
      return true;
    }
    return false;
  };

  const toTokens = (text: string) => mentionsToTokens(text, mentioned);

  const reset = () => {
    setMentioned([]);
    lastPick.current = undefined;
    setTrigger(undefined);
    setDismissedStart(undefined);
  };

  return {
    inputRef,
    handleChange,
    handleSelect,
    consumePickerKey,
    close,
    selectUser,
    toTokens,
    reset,
    pickerOpen,
    users,
    highlighted,
    setHighlighted,
    isLoading: usersLoadable.isLoading,
  };
};
