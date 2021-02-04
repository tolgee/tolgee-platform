import {container, singleton} from 'tsyringe';
import {AbstractLoadableActions, StateWithLoadables} from "../AbstractLoadableActions";
import {importExportService} from "../../service/importExportService";

export class ImportExportState extends StateWithLoadables<ImportExportActions> {

}

@singleton()
export class ImportExportActions extends AbstractLoadableActions<ImportExportState> {
    constructor() {
        super(new ImportExportState());
    }

    private service = container.resolve(importExportService);

    loadableDefinitions = {
        import: this.createLoadableDefinition(this.service.doImport),
        export: this.createLoadableDefinition(this.service.exportToJsons)
    };

    get prefix(): string {
        return 'IMPORT_EXPORT';
    }

}
