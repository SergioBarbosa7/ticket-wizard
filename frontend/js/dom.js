// Criação de elementos sem innerHTML: todo texto entra como nó de texto, então
// dado vindo da API nunca é interpretado como HTML.
export function el(tag, props = {}, ...filhos) {
  const no = document.createElement(tag);
  for (const [chave, valor] of Object.entries(props)) {
    if (valor == null || valor === false) continue;
    if (chave === 'class') no.className = valor;
    else if (chave === 'dataset') Object.assign(no.dataset, valor);
    else if (chave.startsWith('on')) no.addEventListener(chave.slice(2).toLowerCase(), valor);
    else no.setAttribute(chave, valor === true ? '' : String(valor));
  }
  for (const filho of filhos.flat()) {
    if (filho == null || filho === false) continue;
    no.append(typeof filho === 'object' && 'nodeType' in filho ? filho : document.createTextNode(String(filho)));
  }
  return no;
}

export function esvaziar(no) {
  no.replaceChildren();
}
