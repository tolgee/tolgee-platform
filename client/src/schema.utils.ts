import type { paths } from "./schema.generated";

type ValueOf<T extends Record<any, any>> = T[keyof T];

export type Path = keyof paths;

export type MethodsOf<P extends Path> = keyof paths[P];

export type HeadersOf<
  P extends Path,
  M extends MethodsOf<P>
> = paths[P][M] extends { parameters: { headers: Record<string, any> } }
  ? paths[P][M]["parameters"]["headers"]
  : void;

export type ParamsOf<
  P extends Path,
  M extends MethodsOf<P>
> = paths[P][M] extends { parameters: { path: Record<string, any> } }
  ? paths[P][M]["parameters"]["path"]
  : void;

export type QueryOf<
  P extends Path,
  M extends MethodsOf<P>
> = paths[P][M] extends { parameters: { query?: Record<string, any> } }
  ? Omit<Exclude<paths[P][M]["parameters"]["query"], undefined>, "ak">
  : void;

export type BodyOf<
  P extends Path,
  M extends MethodsOf<P>
> = paths[P][M] extends { requestBody?: { content: Record<string, any> } }
  ? ValueOf<Exclude<paths[P][M]["requestBody"], undefined>["content"]>
  : void;

type ExtractInnerResponse<T extends Record<any, any>> = {
  [TCode in keyof T]: T[TCode] extends { content: Record<string, any> }
    ? ValueOf<T[TCode]["content"]>
    : void;
};

export type ResponseOf<
  P extends Path,
  M extends MethodsOf<P>
> = paths[P][M] extends { responses: Record<any, any> }
  ? ExtractInnerResponse<paths[P][M]["responses"]>
  : void;
