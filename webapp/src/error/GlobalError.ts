export class GlobalError extends Error {
  constructor(public publicInfo: string, public debugInfo?: string, public e?) {
    super();

    // restore prototype chain
    const actualProto = new.target.prototype;

    if (Object.setPrototypeOf) {
      Object.setPrototypeOf(this, actualProto);
    } else {
      // @ts-ignore
      this.__proto__ = actualProto;
    }
  }
}
