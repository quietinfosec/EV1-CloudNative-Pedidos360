import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import { HomeComponent } from './pages/home.component';
import { LoginComponent } from './pages/login.component';
import { AuthCallbackComponent } from './pages/auth-callback.component';
import { ProductosComponent } from './pages/productos.component';
import { PedidosComponent } from './pages/pedidos.component';
import { PerfilComponent } from './pages/perfil.component';
import { RabbitmqLabComponent } from './pages/rabbitmq-lab.component';

export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'auth/callback', component: AuthCallbackComponent },
  { path: 'productos', component: ProductosComponent },
  { path: 'pedidos', component: PedidosComponent, canActivate: [authGuard] },
  { path: 'perfil', component: PerfilComponent, canActivate: [authGuard] },
  { path: 'rabbitmq-lab', component: RabbitmqLabComponent },
  { path: '**', redirectTo: 'home' }
];
