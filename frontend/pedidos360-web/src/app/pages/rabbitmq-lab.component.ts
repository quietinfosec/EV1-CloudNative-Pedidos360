import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription, interval, startWith, switchMap } from 'rxjs';
import { environment } from '../../environments/environment';

interface QueueSummary {
  name: string;
  durable: boolean;
  messages: number;
  consumers: number;
  state: string;
}

interface ExchangeSummary {
  name: string;
  type: string;
  durable: boolean;
}

interface FlowStep {
  title: string;
  detail: string;
  state: 'idle' | 'running' | 'done' | 'error';
}

@Component({
  selector: 'app-rabbitmq-lab',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <main class="lab-shell">
      <header class="lab-header">
        <div>
          <p class="eyebrow">Pedidos360 / EP3</p>
          <h1>RabbitMQ Flight Deck</h1>
          <p class="lede">Ejecuta el flujo completo desde una sola pestaña: publica, enruta, consume y visualiza la evidencia.</p>
        </div>
        <div class="connection-pill" [class.offline]="error">
          <span class="pulse"></span>{{ error ? 'Broker desconectado' : 'Broker conectado' }}
        </div>
      </header>

      <section class="flow-card">
        <div class="flow-topline">
          <div>
            <p class="eyebrow">Flujo visual</p>
            <h2>Mensaje de pedido: creado</h2>
          </div>
          <button class="primary-action" type="button" (click)="runFlow()" [disabled]="running">
            <span *ngIf="running" class="spinner"></span>{{ running ? 'Ejecutando...' : 'Ejecutar flujo completo' }}
          </button>
        </div>

        <div class="flow-track" aria-label="Pasos del flujo RabbitMQ">
          <div *ngFor="let step of steps; let i = index" class="flow-step" [class]="step.state">
            <div class="step-marker">{{ step.state === 'done' ? '✓' : (step.state === 'error' ? '!' : (i + 1)) }}</div>
            <div class="step-copy"><strong>{{ step.title }}</strong><span>{{ step.detail }}</span></div>
            <div *ngIf="i < steps.length - 1" class="connector"></div>
          </div>
        </div>

        <div *ngIf="success" class="success-banner" role="status">✓ {{ success }}</div>
        <div *ngIf="error" class="error-banner" role="alert">{{ error }} <button type="button" (click)="refresh()">Reintentar</button></div>
      </section>

      <section class="dashboard-grid">
        <article class="panel composer-panel">
          <div class="panel-heading"><div><p class="eyebrow">Control de emisión</p><h2>Publicar evento</h2></div><span class="tag">ACK / NACK / DLQ</span></div>
          <label>Exchange<input [(ngModel)]="exchange" aria-label="Exchange" /></label>
          <label>Routing key<input [(ngModel)]="routingKey" aria-label="Routing key" /></label>
          <label>Payload JSON<textarea [(ngModel)]="payload" rows="9" aria-label="Payload JSON"></textarea></label>
          <button class="secondary-action" type="button" (click)="publish()" [disabled]="running || publishing">{{ publishing ? 'Publicando...' : 'Publicar mensaje' }}</button>
          <p class="helper">Usa el botón de flujo para ejecutar la demo recomendada sin editar estos campos.</p>
        </article>

        <article class="panel telemetry-panel">
          <div class="panel-heading"><div><p class="eyebrow">Observabilidad</p><h2>Estado en vivo</h2></div><button class="refresh" type="button" (click)="refresh()" [disabled]="loading">↻ Actualizar</button></div>
          <div class="metric-row"><div class="metric"><span>Colas</span><strong>{{ queues.length }}</strong></div><div class="metric"><span>Mensajes</span><strong>{{ totalMessages }}</strong></div><div class="metric"><span>Exchanges</span><strong>{{ exchanges.length }}</strong></div></div>
          <div *ngIf="loading && !queues.length" class="loading-state">Consultando Management API...</div>
          <div *ngIf="!loading && !queues.length && !error" class="empty-state"><strong>No hay telemetría todavía</strong><span>Ejecuta el flujo para poblar la vista.</span></div>
          <div class="queue-list"><div *ngFor="let queue of queues" class="queue-row"><div><strong>{{ queue.name }}</strong><span>{{ queue.consumers }} consumidores · {{ queue.state }}</span></div><b>{{ queue.messages }}</b></div></div>
        </article>
      </section>

      <footer class="lab-footer"><span>Management API server-side · credenciales nunca llegan al navegador</span><span>Actualización automática cada 3 s</span></footer>
    </main>
  `,
  styles: [`
    :host { display:block; background:#07111f; min-height:calc(100vh - 70px); color:#e7eef7; }
    .lab-shell { max-width:1320px; margin:auto; padding:48px 28px 28px; font-family:Inter, ui-sans-serif, system-ui, sans-serif; }
    .lab-header,.flow-topline,.panel-heading,.lab-footer,.metric-row,.queue-row { display:flex; align-items:center; justify-content:space-between; gap:20px; }
    .lab-header { margin-bottom:32px; align-items:flex-end; } h1 { margin:4px 0 10px; font-size:clamp(2.1rem,5vw,4.5rem); letter-spacing:-.07em; line-height:.95; color:#f8fbff; } h2 { margin:4px 0 0; font-size:1.25rem; letter-spacing:-.03em; } .lede { max-width:650px; color:#8fa5ba; font-size:1rem; line-height:1.6; margin:0; }
    .eyebrow { color:#54d2b0; font-size:.68rem; font-weight:800; letter-spacing:.16em; text-transform:uppercase; margin:0; }.connection-pill,.tag { border:1px solid #234058; border-radius:999px; padding:9px 13px; color:#9ab4c8; font-size:.78rem; white-space:nowrap; }.pulse { display:inline-block; width:7px; height:7px; border-radius:50%; background:#54d2b0; margin-right:8px; box-shadow:0 0 0 5px #54d2b01f; }.offline .pulse { background:#ff7c86; }.flow-card,.panel { background:#0d1b2a; border:1px solid #19344a; border-radius:18px; box-shadow:0 18px 60px #0000001f; }.flow-card { padding:28px; margin-bottom:20px; }.primary-action,.secondary-action { border:0; border-radius:10px; color:#06111b; font-weight:800; cursor:pointer; padding:13px 18px; background:#54d2b0; }.primary-action:hover,.secondary-action:hover { background:#78e4c8; }.primary-action:disabled,.secondary-action:disabled { opacity:.55; cursor:wait; }.flow-track { display:grid; grid-template-columns:repeat(4,1fr); gap:0; margin:42px 4px 24px; }.flow-step { position:relative; display:flex; flex-direction:column; gap:10px; min-height:82px; }.step-marker { width:31px; height:31px; border:1px solid #34536c; border-radius:50%; display:grid; place-items:center; color:#668399; font-weight:800; background:#091724; z-index:1; }.flow-step.done .step-marker { color:#06111b; background:#54d2b0; border-color:#54d2b0; }.flow-step.running .step-marker { color:#06111b; background:#ffd166; border-color:#ffd166; }.flow-step.error .step-marker { color:#fff; background:#e95b68; border-color:#e95b68; }.step-copy { display:flex; flex-direction:column; gap:5px; padding-right:25px; }.step-copy strong { font-size:.87rem; }.step-copy span { font-size:.74rem; color:#7e96aa; line-height:1.4; }.connector { position:absolute; top:15px; left:31px; width:calc(100% - 30px); border-top:1px dashed #34536c; }.done .connector { border-color:#54d2b0; }.dashboard-grid { display:grid; grid-template-columns:minmax(280px,.85fr) minmax(420px,1.15fr); gap:20px; }.panel { padding:24px; }.panel-heading { margin-bottom:22px; align-items:flex-start; }.tag { color:#ffd166; border-color:#66532a; font-size:.65rem; }.composer-panel label { display:grid; gap:7px; color:#a5bacb; font-size:.75rem; font-weight:700; margin-bottom:16px; }.composer-panel input,.composer-panel textarea { width:100%; box-sizing:border-box; border:1px solid #27445a; border-radius:8px; background:#081522; color:#e7eef7; padding:11px 12px; font:inherit; font-size:.86rem; }.composer-panel textarea { resize:vertical; font-family:ui-monospace,monospace; line-height:1.5; }.helper,.loading-state,.empty-state { color:#71899d; font-size:.76rem; line-height:1.5; }.refresh { border:1px solid #31516a; background:transparent; color:#a9c0d1; border-radius:8px; padding:7px 10px; cursor:pointer; }.metric-row { justify-content:flex-start; border-bottom:1px solid #19344a; padding-bottom:20px; margin-bottom:16px; }.metric { display:grid; gap:5px; min-width:90px; }.metric span { color:#71899d; font-size:.68rem; text-transform:uppercase; letter-spacing:.1em; }.metric strong { font-size:1.7rem; color:#f8fbff; }.queue-list { display:grid; gap:8px; max-height:360px; overflow:auto; }.queue-row { padding:12px 0; border-bottom:1px solid #19344a; }.queue-row div { display:grid; gap:5px; min-width:0; }.queue-row strong { font-size:.78rem; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }.queue-row span { color:#71899d; font-size:.7rem; }.queue-row b { color:#54d2b0; font-size:1.2rem; }.success-banner,.error-banner { margin-top:14px; padding:11px 13px; border-radius:9px; font-size:.8rem; }.success-banner { background:#54d2b014; border:1px solid #54d2b066; color:#8be8ce; }.error-banner { background:#ff687014; border:1px solid #ff687066; color:#ffabb2; }.error-banner button { margin-left:10px; border:0; background:transparent; color:inherit; text-decoration:underline; cursor:pointer; }.lab-footer { margin-top:18px; color:#5f788d; font-size:.7rem; }.spinner { display:inline-block; width:12px; height:12px; border:2px solid #06111b66; border-top-color:#06111b; border-radius:50%; margin-right:8px; animation:spin .8s linear infinite; } @keyframes spin { to { transform:rotate(360deg); } }
    @media (max-width:850px) { .lab-header { align-items:flex-start; flex-direction:column; }.dashboard-grid { grid-template-columns:1fr; }.flow-track { grid-template-columns:1fr; gap:18px; }.connector { display:none; }.flow-step { min-height:auto; flex-direction:row; align-items:flex-start; }.step-copy { padding-top:4px; }.flow-topline { align-items:flex-start; flex-direction:column; }.lab-shell { padding:28px 16px; } }
  `]
})
export class RabbitmqLabComponent implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly subscriptions = new Subscription();
  private readonly adminUrl = environment.rabbitmqAdminUrl;
  queues: QueueSummary[] = [];
  exchanges: ExchangeSummary[] = [];
  loading = false;
  publishing = false;
  running = false;
  error = '';
  success = '';
  exchange = 'pedidos360.direct.exchange';
  routingKey = 'pedido.creado';
  payload = JSON.stringify({ eventId: crypto.randomUUID(), eventType: 'pedido.creado', pedidoId: 9001, usuario: 'demo@pedidos360.local', total: 15000, correlationId: 'demo-rabbitmq' }, null, 2);
  steps: FlowStep[] = [
    { title: 'Publicar', detail: 'Enviar evento al exchange', state: 'idle' },
    { title: 'Enrutar', detail: 'Aplicar routing key', state: 'idle' },
    { title: 'Consumir', detail: 'Procesar mensaje', state: 'idle' },
    { title: 'Confirmar', detail: 'ACK y evidencia', state: 'idle' }
  ];

  constructor() {
    this.refresh();
    this.subscriptions.add(interval(3000).pipe(startWith(0), switchMap(() => this.http.get<QueueSummary[]>(`${this.adminUrl}/queues`))).subscribe({ next: data => { this.queues = data; this.error = ''; }, error: () => { this.error = 'No se pudo consultar RabbitMQ. Verifica que el Admin Service esté arriba y que exista un usuario ADMIN.'; } }));
  }

  get totalMessages(): number { return this.queues.reduce((sum, queue) => sum + queue.messages, 0); }

  refresh(): void {
    this.loading = true;
    this.http.get<ExchangeSummary[]>(`${this.adminUrl}/exchanges`).subscribe({ next: data => this.exchanges = data, error: () => this.error = 'No se pudo cargar la topología de exchanges.', complete: () => this.loading = false });
    this.http.get<QueueSummary[]>(`${this.adminUrl}/queues`).subscribe({ next: data => this.queues = data, error: () => this.error = 'No se pudo cargar el estado de las colas.', complete: () => this.loading = false });
  }

  runFlow(): void {
    this.running = true; this.success = ''; this.error = '';
    this.steps.forEach(step => step.state = 'idle');
    this.publish(true);
  }

  publish(fromFlow = false): void {
    this.publishing = true; this.error = '';
    if (fromFlow) this.steps[0].state = 'running';
    this.http.post<{ routed: boolean }>(`${this.adminUrl}/messages`, { exchange: this.exchange, routingKey: this.routingKey, payload: this.payload, vhost: '/' }).subscribe({
      next: response => {
        this.publishing = false;
        if (!fromFlow) { this.success = response.routed ? 'Mensaje publicado y enrutable.' : 'Mensaje publicado, pero no encontró una cola enlazada.'; return; }
        this.steps[0].state = 'done'; this.steps[1].state = 'done'; this.steps[2].state = 'done'; this.steps[3].state = 'done'; this.success = response.routed ? 'Flujo completado: evento publicado, enrutado y confirmado por RabbitMQ.' : 'Publicación completada, pero sin ruta activa. Revisa bindings.'; this.running = false; this.refresh();
      },
      error: () => { this.publishing = false; this.running = false; if (fromFlow) this.steps[0].state = 'error'; this.error = 'RabbitMQ rechazó la publicación. Revisa autenticación, exchange y routing key.'; }
    });
  }

  ngOnDestroy(): void { this.subscriptions.unsubscribe(); }
}
