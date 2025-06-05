import React from 'react';
import { GlossaryContextHolder } from 'tg.ee.module/glossary/hooks/GlossaryProvider';

export const useGlossaryContext = () => React.useContext(GlossaryContextHolder);
