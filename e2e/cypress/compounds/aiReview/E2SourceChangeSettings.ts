import { gcyAdvanced } from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';

export class E2SourceChangeSettings {
  getSelect(field: string, selected?: string) {
    if (selected) {
      return gcyAdvanced({ value: 'source-change-select', field, selected });
    }
    return gcyAdvanced({ value: 'source-change-select', field });
  }

  choose(field: string, action: string) {
    this.getSelect(field).click();
    gcyAdvanced({ value: 'source-change-select-item', action }).click();
    waitForGlobalLoading();
    return this;
  }

  getLockedHint(field: string) {
    return gcyAdvanced({ value: 'source-change-locked-hint', field });
  }
}
