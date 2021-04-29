import {singleton} from 'tsyringe';
import {AbstractLoadableActions, StateWithLoadables} from "../AbstractLoadableActions";
import {ImportExportService} from "../../service/ImportExportService";

export class ImportExportState extends StateWithLoadables<ImportExportActions> {

}

@singleton()
export class ImportExportActions extends AbstractLoadableActions<ImportExportState> {
    constructor(private service: ImportExportService) {
        super(new ImportExportState());
    }
    
    loadableDefinitions = {
        export: this.createLoadableDefinition(this.service.exportToJsons)
    };

    get prefix(): string {
        return 'IMPORT_EXPORT';
    }

}
