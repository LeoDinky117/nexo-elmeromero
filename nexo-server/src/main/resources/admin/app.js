'use strict';

const $ = id => document.getElementById(id);
let premios = [];
let metricas = null;
let cargando = false;

function elemento(tag, texto, clase) {
  const node = document.createElement(tag);
  if (texto !== undefined) node.textContent = texto;
  if (clase) node.className = clase;
  return node;
}

async function api(path, options = {}) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 15000);
  try {
    const esFormularioArchivo = typeof FormData !== 'undefined' && options.body instanceof FormData;
    const response = await fetch(path, {
      ...options,
      credentials: 'same-origin',
      headers: { ...(options.body && !esFormularioArchivo ? { 'Content-Type': 'application/json' } : {}), ...options.headers },
      signal: controller.signal,
      cache: 'no-store'
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      const error = new Error(data.mensaje || 'No se pudo completar la consulta.');
      error.status = response.status;
      throw error;
    }
    return data;
  } catch (error) {
    if (error.name === 'AbortError') throw new Error('La consulta tardó demasiado. Intenta de nuevo.');
    if (error instanceof TypeError) throw new Error('No se pudo conectar con NEXO. Comprueba que el servidor esté disponible.');
    throw error;
  } finally {
    clearTimeout(timeout);
  }
}

function mostrarAcceso(mensaje = '') {
  premios = [];
  metricas = null;
  $('catalog').replaceChildren();
  $('metrics').replaceChildren();
  $('community-metrics').replaceChildren();
  $('monthly-signups').replaceChildren();
  $('login-view').hidden = false;
  $('panel-view').hidden = true;
  $('login-form').elements.clave.value = '';
  $('login-error').textContent = mensaje;
  $('panel-error').textContent = '';
  $('notice').textContent = '';
}

function pagina(nombre) {
  const resumen = nombre === 'resumen';
  $('resumen-page').hidden = !resumen;
  $('premios-page').hidden = resumen;
  document.querySelectorAll('nav [data-page]').forEach(button => {
    const activo = button.dataset.page === nombre;
    button.classList.toggle('active', activo);
    if (activo) button.setAttribute('aria-current', 'page');
    else button.removeAttribute('aria-current');
  });
  $('page-title').textContent = resumen ? 'Vista general' : 'Premios';
  $('page-title').focus();
}

document.querySelectorAll('[data-page]').forEach(button => button.addEventListener('click', () => pagina(button.dataset.page)));

async function cargarPanel() {
  if (cargando) return;
  cargando = true;
  const refresh = $('refresh');
  refresh.disabled = true;
  refresh.textContent = 'Actualizando…';
  $('panel-error').textContent = '';
  $('data-status').textContent = 'Consultando datos…';
  try {
    const [lista, resumen] = await Promise.all([
      api('/admin/recompensas'),
      api('/admin/recompensas/metricas')
    ]);
    premios = lista;
    metricas = resumen;
    renderizar();
    $('data-status').textContent = 'Datos consultados · ' + horaActual();
    $('updated').textContent = 'Actualizado ' + horaActual();
    $('data-status').classList.remove('stale');
  } catch (error) {
    if (error.status === 401 || error.status === 403) mostrarAcceso(error.message);
    else {
      $('panel-error').textContent = error.message;
      $('data-status').textContent = 'No se pudo actualizar';
      $('data-status').classList.add('stale');
    }
  } finally {
    cargando = false;
    refresh.disabled = false;
    refresh.textContent = '↻ Actualizar';
  }
}

function horaActual() {
  return new Date().toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' });
}

function formatearNumero(numero) {
  return new Intl.NumberFormat('es-MX').format(Number(numero) || 0);
}

function renderizarMetricas() {
  const contenedor = $('metrics');
  contenedor.replaceChildren();
  const tarjetas = [
    [metricas.Usuarios, 'Cuentas registradas', 'Total de usuarios'],
    [metricas.PremiosActivos, 'Premios publicados', 'Disponibles en la app'],
    [metricas.Desbloqueos, 'Premios obtenidos', 'Desbloqueos acumulados'],
    [metricas.PuntosAcumulados, 'Puntos acumulados', 'Entre todas las cuentas']
  ];
  tarjetas.forEach(([valor, titulo, detalle]) => {
    const card = elemento('div', undefined, 'metric');
    card.append(elemento('span', titulo), elemento('strong', formatearNumero(valor)), elemento('small', detalle));
    contenedor.append(card);
  });

  const porcentaje = metricas.Usuarios ? (Number(metricas.UsuariosActivos || 0) * 100 / Number(metricas.Usuarios)).toFixed(1) : '0.0';
  const retencion = metricas.CohorteRetencion30Dias
    ? `${Number(metricas.Retencion30Dias || 0).toFixed(1)}% de ${formatearNumero(metricas.CohorteRetencion30Dias)} usuarios registrados hace 30–60 días`
    : 'Aún no hay una cohorte de 30 días suficiente';
  const comunidad = [
    [metricas.UsuariosActivos, 'Usuarios activos en la app', `${porcentaje}% del total · actividad registrada en los últimos 30 días`],
    [metricas.UsuariosInactivos, 'Usuarios inactivos', 'Sin actividad de la app registrada en los últimos 30 días'],
    [metricas.MetasCompletadas, 'Metas completadas', 'Metas con fecha de cumplimiento registrada'],
    [`${Number(metricas.PromedioDiasMeta || 0).toFixed(1)} días`, 'Tiempo promedio por meta', 'Desde su creación hasta completarse'],
    [`${Number(metricas.Retencion30Dias || 0).toFixed(1)}%`, 'Retención a 30 días', retencion],
    [metricas.UsuariosConRacha, 'Rachas activas', 'Racha vigente hoy o hasta ayer'],
    [`${Number(metricas.RachaPromedio || 0).toFixed(1)} días`, 'Racha promedio', 'Entre usuarios con racha vigente'],
    [`${Number(metricas.TiempoRespuestaMs || 0).toFixed(0)} ms`, 'Tiempo de respuesta', 'Consulta de métricas al servidor y la base de datos']
  ];
  $('community-metrics').replaceChildren();
  comunidad.forEach(([valor, titulo, detalle]) => {
    const card = elemento('div', undefined, 'metric');
    card.append(elemento('span', titulo), elemento('strong', typeof valor === 'number' ? formatearNumero(valor) : valor), elemento('small', detalle));
    $('community-metrics').append(card);
  });

  const meses = metricas.RegistrosMensuales || [];
  const maximo = Math.max(1, ...meses.map(item => Number(item.Cantidad) || 0));
  $('monthly-signups').replaceChildren();
  meses.forEach(item => {
    const fila = elemento('div', undefined, 'month-row');
    const etiqueta = new Date(`${item.Mes}-02T12:00:00`).toLocaleDateString('es-MX', { month: 'short', year: 'numeric' });
    const barra = elemento('div', undefined, 'month-track');
    const relleno = elemento('span', undefined, 'month-fill');
    relleno.style.width = `${Math.max(item.Cantidad ? 4 : 0, Number(item.Cantidad) * 100 / maximo)}%`;
    barra.append(relleno);
    fila.append(elemento('span', etiqueta), barra, elemento('strong', formatearNumero(item.Cantidad)));
    $('monthly-signups').append(fila);
  });
}

function crearArte(premio) {
  const tipo = premio.Tipo === 'TEMA' ? 'tema' : 'accesorio';
  const esImagen = premio.Tipo === 'ICONO' && /^\/admin\/icons\/[0-9a-f-]{36}\.(png|jpg|webp)$/i.test(String(premio.ValorVisual));
  const arte = elemento('div', esImagen || premio.Tipo === 'TEMA' ? undefined : premio.ValorVisual, `badge-art ${tipo}`);
  arte.setAttribute('aria-hidden', 'true');
  if (esImagen) {
    const imagen = document.createElement('img');
    imagen.src = premio.ValorVisual;
    imagen.alt = '';
    arte.append(imagen);
  }
  if (premio.Tipo === 'TEMA') {
    const colores = String(premio.ValorVisual).split('|');
    if (colores.length === 4 && colores.every(color => /^#[0-9a-f]{6}$/i.test(color))) {
      arte.style.background = `linear-gradient(135deg, ${colores[0]}, ${colores[3]})`;
    }
  }
  return arte;
}

function renderizarCatalogo() {
  const query = $('search').value.trim().toLocaleLowerCase('es');
  const filtrados = premios.filter(p => `${p.Nombre} ${p.Descripcion} ${p.Codigo}`.toLocaleLowerCase('es').includes(query));
  $('catalog').replaceChildren();
  $('count').textContent = `${formatearNumero(filtrados.length)} de ${formatearNumero(premios.length)} premios`;

  filtrados.forEach(premio => {
    const card = elemento('article', undefined, 'prize');
    const top = elemento('div', undefined, 'top');
    top.append(crearArte(premio), elemento('span', premio.Tipo === 'TEMA' ? 'Paleta' : 'Icono', 'pill'));
    card.append(top, elemento('small', premio.Codigo, 'muted'), elemento('h3', premio.Nombre));
    if (premio.Descripcion) card.append(elemento('p', premio.Descripcion));
    card.append(elemento('div', `${formatearNumero(premio.CostoPuntos)} puntos`, 'requirement'));
    $('catalog').append(card);
  });
  if (!filtrados.length) $('catalog').append(elemento('p', 'No hay premios que coincidan con la búsqueda.', 'empty'));
}

function renderizar() {
  renderizarMetricas();
  renderizarCatalogo();
}

$('login-form').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const button = event.submitter;
  button.disabled = true;
  $('login-error').textContent = '';
  try {
    const clave = form.elements.clave.value;
    await api('/admin/sesion', { method: 'POST', body: JSON.stringify({ clave }) });
    form.elements.clave.value = '';
    $('login-view').hidden = true;
    $('panel-view').hidden = false;
    pagina('resumen');
    await cargarPanel();
  } catch (error) {
    $('login-error').textContent = error.message;
  } finally {
    form.elements.clave.value = '';
    button.disabled = false;
  }
});

async function cerrarSesion() {
  try { await api('/admin/sesion/salir', { method: 'POST', body: '{}' }); }
  catch { /* Se limpia la vista aunque la sesión ya haya vencido o la red no responda. */ }
  mostrarAcceso('');
}

$('logout').addEventListener('click', cerrarSesion);
$('refresh').addEventListener('click', cargarPanel);
$('search').addEventListener('input', renderizarCatalogo);
$('create').addEventListener('click', abrirEditor);
$('hero-create').addEventListener('click', () => { pagina('premios'); abrirEditor(); });
$('close-editor').addEventListener('click', () => $('editor').close());
$('cancel-editor').addEventListener('click', () => $('editor').close());

function actualizarAyudaVisual() {
  const tema = $('reward-type').value === 'TEMA';
  const archivo = $('reward-image').files[0];
  $('visual-label').hidden = tema;
  $('image-label').hidden = tema;
  $('theme-picker').hidden = !tema;
  $('reward-visual').required = !tema && !archivo;
  $('reward-visual').disabled = tema || Boolean(archivo);
  $('reward-visual').placeholder = 'Ej. 🐱';
  $('visual-help').textContent = 'Usa un emoji o símbolo de hasta 12 caracteres, o importa una imagen.';
  if (tema) {
    const valores = $('reward-visual').value.split('|');
    const iniciales = ['#35C6C8', '#071820', '#102A35', '#75E0D3'];
    [1, 2, 3, 4].forEach((n, index) => {
      const campo = $(`theme-color-${n}`);
      campo.value = /^#[0-9a-f]{6}$/i.test(valores[index] || '') ? valores[index] : iniciales[index];
    });
    actualizarValorTema();
  }
  actualizarVistaPrevia();
}

function actualizarValorTema() {
  $('reward-visual').value = [1, 2, 3, 4].map(n => $(`theme-color-${n}`).value.toUpperCase()).join('|');
  actualizarVistaPrevia();
}

function actualizarVistaPrevia() {
  const tipo = $('reward-type').value;
  const premio = {
    Tipo: tipo,
    ValorVisual: $('reward-visual').value.trim(),
    Nombre: $('reward-name').value.trim() || 'Nombre del premio',
    Descripcion: $('reward-description').value.trim() || 'Aquí aparecerá su descripción.',
    CostoPuntos: Number($('reward-cost').value) || 0
  };
  const row = elemento('div', undefined, 'feature');
  const info = elemento('div');
  info.append(elemento('strong', premio.Nombre), elemento('p', premio.Descripcion), elemento('p', `${formatearNumero(premio.CostoPuntos)} puntos`));
  const arte = crearArte(premio);
  const archivo = $('reward-type').value === 'ICONO' && $('reward-image').files[0];
  if (archivo) {
    const imagen = document.createElement('img');
    imagen.src = URL.createObjectURL(archivo);
    imagen.alt = '';
    arte.replaceChildren(imagen);
  }
  row.append(arte, info);
  $('reward-preview').replaceChildren(row);
}

function abrirEditor() {
  $('reward-form').reset();
  $('reward-cost').value = '100';
  $('save-error').textContent = '';
  actualizarAyudaVisual();
  $('editor').showModal();
  $('reward-type').focus();
}

$('reward-type').addEventListener('change', () => {
  $('reward-visual').value = '';
  $('reward-image').value = '';
  actualizarAyudaVisual();
});
['reward-visual', 'reward-name', 'reward-description', 'reward-cost'].forEach(id => $(id).addEventListener('input', actualizarVistaPrevia));
[1, 2, 3, 4].forEach(n => $(`theme-color-${n}`).addEventListener('input', actualizarValorTema));
$('reward-image').addEventListener('change', () => {
  const archivo = $('reward-image').files[0];
  $('save-error').textContent = '';
  const extensionValida = /\.(png|jpe?g|webp)$/i.test(archivo?.name || '');
  const tipoValido = !archivo?.type || ['image/png', 'image/jpeg', 'image/webp'].includes(archivo.type);
  if (archivo && (archivo.size > 2 * 1024 * 1024 || !extensionValida || !tipoValido)) {
    $('save-error').textContent = 'Elige un PNG, JPG/JPEG o WebP de máximo 2 MB.';
    $('reward-image').value = '';
  }
  actualizarAyudaVisual();
});

$('reward-form').addEventListener('submit', async event => {
  event.preventDefault();
  const button = $('save');
  button.disabled = true;
  button.textContent = 'Guardando…';
  $('save-error').textContent = '';
  const body = {
    Codigo: $('reward-code').value.trim(),
    Nombre: $('reward-name').value.trim(),
    Descripcion: $('reward-description').value.trim(),
    Tipo: $('reward-type').value,
    CostoPuntos: Number($('reward-cost').value),
    ValorVisual: $('reward-visual').value.trim()
  };
  try {
    const archivo = $('reward-type').value === 'ICONO' && $('reward-image').files[0];
    if (archivo) {
      if (archivo.size > 2 * 1024 * 1024) throw new Error('La imagen no puede superar 2 MB.');
      const formulario = new FormData();
      formulario.append('icono', archivo);
      const subida = await api('/admin/recompensas/iconos', { method: 'POST', body: formulario });
      body.ValorVisual = subida.ruta;
    }
    await api('/admin/recompensas', { method: 'POST', body: JSON.stringify(body) });
    $('editor').close();
    $('notice').textContent = 'Premio agregado. Ya puede aparecer en el catálogo de recompensas de la app.';
    await cargarPanel();
  } catch (error) {
    if (error.status === 401 || error.status === 403) mostrarAcceso(error.message);
    else $('save-error').textContent = error.message;
  } finally {
    button.disabled = false;
    button.textContent = 'Guardar premio';
  }
});

// No se restaura ninguna clave al cargar la página: la sesión vive en una cookie
// HttpOnly emitida por el servidor, inaccesible para este JavaScript.
