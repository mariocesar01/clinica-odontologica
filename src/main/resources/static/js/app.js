(() => {
  'use strict';
  const path = location.pathname;
  for (const link of document.querySelectorAll('.sidebar-nav a[data-path]')) {
    const base = link.dataset.path;
    // /chat is the common parent for chat/conversas and chat/minha.
    if (path === base || (base !== '/home' && path.startsWith(base + '/'))) {
      link.classList.add('active');
      link.setAttribute('aria-current', 'page');
    }
  }
  const heading = document.querySelector('.page-content h1');
  const crumb = document.getElementById('crumb-page');
  if (heading && crumb) crumb.textContent = heading.textContent.trim();
  const today = document.getElementById('today-label');
  if (today) today.textContent = new Intl.DateTimeFormat('pt-BR',{day:'2-digit',month:'long',year:'numeric'}).format(new Date());
  const menuButton = document.getElementById('mobile-menu-button');
  function closeMenu() { document.body.classList.remove('menu-open'); menuButton?.setAttribute('aria-expanded','false'); }
  menuButton?.addEventListener('click',() => {
    const open = document.body.classList.toggle('menu-open');
    menuButton.setAttribute('aria-expanded', String(open));
  });
  document.getElementById('mobile-overlay')?.addEventListener('click',closeMenu);
  window.addEventListener('keydown',e=>{if(e.key==='Escape')closeMenu();});
  // Status text is transformed into a neutral semantic pill, without altering the underlying value.
  const states = new Set(['AGENDADA','CONFIRMADA','AGUARDANDO','EM_ATENDIMENTO','REALIZADA','CANCELADA','PENDENTE','PAGO','CANCELADO','ATIVO','AFASTADO','FOLGA']);
  const readable = {EM_ATENDIMENTO:'Em atendimento',AGUARDANDO:'Aguardando',AGENDADA:'Agendada',CONFIRMADA:'Confirmada',REALIZADA:'Realizada',CANCELADA:'Cancelada',PENDENTE:'Pendente',PAGO:'Pago',CANCELADO:'Cancelado',ATIVO:'Ativo',AFASTADO:'Afastado',FOLGA:'Folga'};
  for(const el of document.querySelectorAll('td, dd')) {
    if (el.children.length || !states.has(el.textContent.trim())) continue;
    const state=el.textContent.trim();
    const pill=document.createElement('span');pill.className='status-pill';pill.dataset.status=state;pill.textContent=readable[state];
    el.replaceChildren(pill);
  }
  for (const form of document.querySelectorAll('form')) {
    if (!form.hasAttribute('method') || form.getAttribute('method').toLowerCase()!=='post') continue;
    const action=(form.getAttribute('action')||'');
    if ((action.includes('/excluir/') || action.includes('/cancelar/')) && !form.querySelector('[onclick*=\"confirm\"]')) {
      form.addEventListener('submit', e => {
        if (!confirm(action.includes('/cancelar/') ? 'Deseja cancelar esta consulta?' : 'Confirma a exclusão deste registro?')) e.preventDefault();
      });
    }
  }
  const chat=document.getElementById('chat-mensagens');
  if (chat) chat.scrollTop=chat.scrollHeight;
})();
