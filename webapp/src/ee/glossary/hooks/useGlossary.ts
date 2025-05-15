import { components } from 'tg.service/apiSchema.generated';
import { useGlossaryContext } from 'tg.ee.module/glossary/hooks/GlossaryContext';

export const useGlossary = (): components['schemas']['GlossaryModel'] => {
  return useGlossaryContext((c) => c.glossary);
};
