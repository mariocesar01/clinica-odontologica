(() => {
  const quadro = document.getElementById('chat-mensagens');
  if (!quadro) return;
  const id = quadro.dataset.conversaId;
  if (!/^\d+$/.test(id)) return;
  let ultimo = '';
  async function atualizar() {
    if (document.hidden) return;
    try {
      const resposta = await fetch('/api/chat/conversas/' + id + '/mensagens', { credentials: 'same-origin' });
      if (!resposta.ok) return;
      const mensagens = await resposta.json();
      const chave = mensagens.map(m => m.id + ':' + (m.lidaEm || '')).join('|');
      if (chave === ultimo) return;
      ultimo = chave;
      const noFinal = quadro.scrollHeight - quadro.scrollTop - quadro.clientHeight < 80;
      quadro.replaceChildren();
      if (!mensagens.length) {
        const vazio = document.createElement('p');
        vazio.textContent = 'Nenhuma mensagem enviada.';
        quadro.append(vazio);
      }
      for (const m of mensagens) {
        const artigo = document.createElement('article');
        artigo.className = 'chat-mensagem' + (String(m.remetenteId) === quadro.dataset.usuarioId ? ' own' : '');
        const autor = document.createElement('strong'); autor.textContent = m.remetente;
        const data = document.createElement('small'); data.textContent = (m.enviadaEm || '').replace('T',' ').slice(0,16);
        const corpo = document.createElement('p'); corpo.textContent = m.texto;
        artigo.append(autor, data, corpo);
        quadro.append(artigo);
      }
      if (noFinal) quadro.scrollTop = quadro.scrollHeight;
    } catch (_) { /* A tela segue acessível mesmo sem atualização automática. */ }
  }
  atualizar();
  setInterval(atualizar, 5000);
})();
