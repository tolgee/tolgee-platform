import { container, singleton } from 'tsyringe';
import { ApiV1HttpService } from './http/ApiV1HttpService';
import { MessageService } from './MessageService';
import React from 'react';
import { ApiSchemaHttpService } from './http/ApiSchemaHttpService';

const http = container.resolve(ApiV1HttpService);
const schemaHttpService = container.resolve(ApiSchemaHttpService);

@singleton()
export class ImportExportService {
  constructor(private messaging: MessageService) {}

  readonly exportToJsons = async (repositoryId: number) =>
    http.getFile('repository/' + repositoryId + '/export/jsonZip');
}
