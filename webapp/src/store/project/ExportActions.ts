import { singleton } from 'tsyringe';

import { ImportExportService } from 'tg.service/ImportExportService';

import {
  AbstractLoadableActions,
  StateWithLoadables,
} from '../AbstractLoadableActions';

export class ExportState extends StateWithLoadables<ExportActions> {}

@singleton()
export class ExportActions extends AbstractLoadableActions<ExportState> {
  constructor(private service: ImportExportService) {
    super(new ExportState());
  }

  loadableDefinitions = {
    export: this.createLoadableDefinition(this.service.exportToJsons),
  };

  get prefix(): string {
    return 'EXPORT';
  }
}
