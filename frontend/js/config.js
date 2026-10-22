// Endereço do API Gateway. Vazio = mesma origem (o nginx faz o proxy de /api/).
// Para abrir o frontend sem nginx, defina antes de carregar a página, por exemplo:
//   window.API_URL = 'http://localhost:8080';
window.API_URL = window.API_URL ?? '';
