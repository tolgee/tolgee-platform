export class DispatchService {
  private _store;

  public set store(store) {
    this._store = store;
  }

  dispatch(action: {
    type: string;
    payload: any;
    meta?: Record<string, unknown>;
    params?: any[];
  }): void {
    if (this._store !== undefined) {
      this._store.dispatch(action);
      return;
    }
    throw new Error(
      'Store is not initialized yet! Please set store on dispatch service first!'
    );
  }
}

export const dispatchService = new DispatchService();
