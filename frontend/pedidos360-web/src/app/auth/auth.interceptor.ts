import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const isProtectedApi = request.url.startsWith(environment.apiBaseUrl)
    || request.url.startsWith(environment.rabbitmqAdminUrl);
  if (!isProtectedApi) {
    return next(request);
  }

  const token = inject(AuthService).getAccessToken();
  if (!token) {
    return next(request);
  }

  return next(request.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  }));
};
