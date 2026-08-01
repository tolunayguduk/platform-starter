/** Shape of GlobalExceptionHandler's JSON error body (platform-error-starter). */
export interface ApiErrorBody {
  errorCode: string;
  message: string;
  timestamp: string;
  traceId: string | null;
  path: string;
}
