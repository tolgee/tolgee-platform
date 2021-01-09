import {singleton} from 'tsyringe';

@singleton()
export class dispatchService {
    private _store;

    public set store(store) {
        this._store = store;
    }

    dispatch(action: { type: string, payload: any, meta?: object, params?: any[] }): void {
        if (this._store !== undefined) {
            this._store.dispatch(action);
            return;
        }
        throw new Error('Store is not initialized yet! Please set store on dispatch service first!');
    }
}