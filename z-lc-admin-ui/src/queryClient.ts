import { QueryClient } from '@tanstack/react-query';
import { isApiError } from '@/api/client';

/**
 * Single react-query client.
 *
 * Errors are surfaced by the per-call `onError` handlers where the UI can be
 * specific; the defaults here keep retries conservative because several z-lc
 * write endpoints are not idempotent (`/runtime/create` would insert twice).
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        // Never retry a business-rule rejection: it will fail identically.
        if (isApiError(error) && error.code >= 400 && error.code < 500) return false;
        return failureCount < 1;
      },
      staleTime: 15_000,
      refetchOnWindowFocus: false,
      gcTime: 5 * 60_000,
    },
    mutations: {
      retry: 0,
    },
  },
});
