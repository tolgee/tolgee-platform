import { container, singleton } from 'tsyringe';

import { MessageService } from './MessageService';
import { ApiSchemaHttpService } from './http/ApiSchemaHttpService';
import { ApiV1HttpService } from './http/ApiV1HttpService';

const http = container.resolve(ApiV1HttpService);
container.resolve(ApiSchemaHttpService);

@singleton()
export class ImportExportService {
  constructor(private messaging: MessageService) {}

  readonly exportToJsons = async (projectId: number) =>
    http.getFile('project/' + projectId + '/export/jsonZip');
}
