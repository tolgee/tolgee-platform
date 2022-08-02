import { messageService, MessageService } from './MessageService';
import { apiV1HttpService } from './http/ApiV1HttpService';

export class ImportExportService {
  constructor(private messaging: MessageService) {}

  readonly exportToJsons = async (projectId: number) =>
    apiV1HttpService.getFile('project/' + projectId + '/export/jsonZip');
}

export const importExportService = new ImportExportService(messageService);
