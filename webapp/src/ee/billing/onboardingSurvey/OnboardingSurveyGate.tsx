/**
 * Stub onboarding survey gate — used when the billing repo is not present.
 * When the billing repo exists, Vite's alias overrides this with the real
 * implementation from billing/frontend/src/onboardingSurvey/OnboardingSurveyGate.tsx.
 *
 * It must render its children. This wraps the entire authenticated app, so a stub
 * that rendered nothing would blank every build made without the billing repo.
 */
import { FC, ReactNode } from 'react';

export const OnboardingSurveyGate: FC<{ children: ReactNode }> = ({
  children,
}) => <>{children}</>;
