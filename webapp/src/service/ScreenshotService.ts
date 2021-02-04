import {singleton} from 'tsyringe';
import {ApiHttpService} from './apiHttpService';
import {ErrorResponseDTO, ScreenshotDTO} from './response.types';

type UploadResult = { stored: ScreenshotDTO[], errors: ErrorResponseDTO[] };

@singleton()
export class ScreenshotService {
    constructor(private http: ApiHttpService) {
    }

    async getForKey(repositoryId, key: string): Promise<ScreenshotDTO[]> {
        return this.http.post(`repository/${repositoryId}/screenshots/get`, {key})
    }

    async upload(files: Blob[], repositoryId: number, key: string): Promise<UploadResult> {
        const responses = await Promise.all(files.map(async file => {
            const formData = new FormData();
            formData.append("screenshot", file);
            formData.append("key", key);
            return await this.http.postMultipart(`repository/${repositoryId}/screenshots`, formData)
                .then(r => ({stored: r}))
                .catch(e => ({error: e})) as { error?: ErrorResponseDTO, stored?: ScreenshotDTO }
        }));

        return responses.reduce((acc, curr) => ({
            stored: curr.stored ? [...acc.stored, curr?.stored] : acc.stored,
            errors: curr.error ? [...acc.errors, curr?.error] : acc.errors
        }), {errors: [], stored: []} as UploadResult);
    }


    async delete(id: number) {
        await this.http.delete(`repository/screenshots/${id}`);
        return id;
    }
}