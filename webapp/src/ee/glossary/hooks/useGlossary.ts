import { useGlossaryContext } from 'tg.ee.module/glossary/hooks/useGlossaryContext';

export const useGlossary = () => useGlossaryContext().glossary;
