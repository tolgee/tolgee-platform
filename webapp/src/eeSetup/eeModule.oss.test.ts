/* eslint-disable no-restricted-imports */
import {
  useQaChecksEnabled,
  useQaDisabledLanguageIds,
  getQaChecksFiltersLength,
  getQaChecksFiltersName,
  useUserTaskCount,
  useGlossaryTermHighlights,
  billingMenuItems,
  apps,
  translationPanelAdder,
  glossaryPanelAdder,
  qaChecksPanelAdder,
  useAddDeveloperViewItems,
  useAddBatchOperations,
  useAddProjectMenuItems,
  TaskReference,
  GlossaryTermPreview,
} from './eeModule.oss';

describe('OSS ee stub module', () => {
  it('reports QA checks as disabled', () => {
    expect(useQaChecksEnabled()).toBe(false);
    expect(useQaDisabledLanguageIds().size).toBe(0);
    expect(getQaChecksFiltersLength({} as any)).toBe(0);
    expect(getQaChecksFiltersName({} as any)).toBeUndefined();
  });

  it('exposes no tasks or glossary highlights', () => {
    expect(useUserTaskCount()).toBe(0);
    expect(useGlossaryTermHighlights({} as any)).toHaveLength(0);
  });

  it('contributes no billing menu items or apps', () => {
    expect(billingMenuItems).toHaveLength(0);
    expect(apps).toHaveLength(0);
  });

  it('adders leave existing items untouched', () => {
    const items = [{ id: 1 }, { id: 2 }];
    expect(translationPanelAdder(items as any)).toBe(items);
    expect(glossaryPanelAdder(items as any)).toBe(items);
    expect(qaChecksPanelAdder(items as any)).toBe(items);
    expect(useAddDeveloperViewItems()(items as any)).toBe(items);
    expect(useAddBatchOperations()(items as any)).toBe(items);
    expect(useAddProjectMenuItems()(items as any)).toBe(items);
  });

  it('provides placeholder components for ee-only features', () => {
    expect(TaskReference).toBeTruthy();
    expect(GlossaryTermPreview).toBeTruthy();
  });
});
