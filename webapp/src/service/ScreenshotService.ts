import { singleton } from 'tsyringe';

import { ApiV1HttpService } from './http/ApiV1HttpService';
import { ErrorResponseDto, ScreenshotDTO } from './response.types';

type UploadResult = { stored: ScreenshotDTO[]; errors: ErrorResponseDto[] };

@singleton()
export class ScreenshotService {
  constructor(private http: ApiV1HttpService) {}

  async getForKey(projectId, key: string): Promise<ScreenshotDTO[]> {
    return this.http.post(`project/${projectId}/screenshots/get`, {
      key,
    });
  }

  async upload(
    files: Blob[],
    projectId: number,
    key: string
  ): Promise<UploadResult> {
    const responses = await Promise.all(
      files.map(async (file) => {
        const formData = new FormData();
        formData.append('screenshot', file);
        formData.append('key', key);
        return (await this.http
          .postMultipart(`project/${projectId}/screenshots`, formData)
          .then((r) => ({ stored: r }))
          .catch((e) => ({ error: e }))) as {
          error?: ErrorResponseDto;
          stored?: ScreenshotDTO;
        };
      })
    );

    return responses.reduce(
      (acc, curr) => ({
        stored: curr.stored ? [...acc.stored, curr?.stored] : acc.stored,
        errors: curr.error ? [...acc.errors, curr?.error] : acc.errors,
      }),
      { errors: [], stored: [] } as UploadResult
    );
  }

  async delete(id: number) {
    await this.http.delete(`project/screenshots/${id}`);
    return id;
  }
}
