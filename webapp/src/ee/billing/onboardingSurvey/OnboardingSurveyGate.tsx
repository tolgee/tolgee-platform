/**
 * Stub onboarding survey gate — used when the billing repo is not present.
 * When the billing repo exists, Vite's alias overrides this with the real
 * implementation from billing/frontend/src/onboardingSurvey/OnboardingSurveyGate.tsx.
 */
import { ReactElement } from 'react';
import { OnboardingSurveyGateProps } from 'eeSetup/EeModuleType';

export const OnboardingSurveyGate = ({
  children,
}: OnboardingSurveyGateProps): ReactElement | null => <>{children}</>;
