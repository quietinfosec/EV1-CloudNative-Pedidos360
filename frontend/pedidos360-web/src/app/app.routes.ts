import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { HomeComponent } from './pages/home.component';
import { LoginComponent } from './pages/login.component';
import { ProductosComponent } from './pages/productos.component';
import { PedidosComponent } from './pages/pedidos.component';
import { PerfilComponent } from './pages/perfil.component';

export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'productos', component: ProductosComponent },
  { path: 'pedidos', component: PedidosComponent, canActivate: [MsalGuard] },
  { path: 'perfil', component: PerfilComponent, canActivate: [MsalGuard] },
  { path: '**', redirectTo: 'home' }
];
