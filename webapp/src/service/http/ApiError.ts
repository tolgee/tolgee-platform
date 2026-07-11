export class ApiError extends Error {
  public handleError: (() => void) | undefined = undefined;
  private handled = false;

  public CUSTOM_VALIDATION?: { [key: string]: any[] };
  public STANDARD_VALIDATION?: { [key: string]: any[] };
  public code?: string;
  public params?: any[];
  public data?: Record<string, any>;

  constructor(message: string, data: Record<string, any> = {}) {
    super(message); // 'Error' breaks prototype chain here
    this.name = 'ApiError';
    this.code = data.code;
    this.params = data.params;
    this.data = data;
    this.CUSTOM_VALIDATION = data.CUSTOM_VALIDATION;
    this.STANDARD_VALIDATION = data.STANDARD_VALIDATION;
    Object.setPrototypeOf(this, new.target.prototype); // restore prototype chain
  }

  public setErrorHandler(handler: () => any) {
    this.handleError = () => {
      if (!this.handled) {
        this.handled = true;
        handler();
      }
    };
  }
}
