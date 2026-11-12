"use strict";
(() => {
 const RASCUNHO="safira.transferencia.rascunho", RESULTADO="safira.transferencia.resultado";
 const texto=(id,v)=>{const e=document.getElementById(id);if(e)e.textContent=v;};
 const moeda=v=>new Intl.NumberFormat("pt-BR",{style:"currency",currency:"BRL"}).format(v);
 const ler=chave=>{try{return JSON.parse(sessionStorage.getItem(chave)||"null");}catch{return null;}};
 function sair(){sessionStorage.removeItem("safira.token");sessionStorage.removeItem("safira.contaId");sessionStorage.removeItem(RASCUNHO);sessionStorage.removeItem(RESULTADO);location.replace("/index.html");}
 async function api(url,opcoes={}) {
  const token=sessionStorage.getItem("safira.token");if(!token){sair();throw Error("Entre novamente.");}
  const resposta=await fetch(url,{...opcoes,headers:{Accept:"application/json",Authorization:`Bearer ${token}`,...opcoes.headers},credentials:"omit",cache:"no-store"});
  let dados=null;try{dados=await resposta.json();}catch{}
  if(resposta.status===401){sair();throw Error("Sessão expirada. Entre novamente.");}
  if(!resposta.ok){const e=Error(dados?.mensagem||dados?.detail||`Não foi possível concluir (HTTP ${resposta.status}).`);e.status=resposta.status;throw e;}
  return dados;
 }
 function quantia(v){
  v=v.trim();if(!/^\d{1,17}([.,]\d{1,2})?$/.test(v))throw Error("Informe o valor sem separador de milhar, com até duas casas decimais.");
  const [inteiro,decimal=""]=v.replace(",",".").split(".");
  const normal=inteiro.replace(/^0+(?=\d)/,"")+"."+decimal.padEnd(2,"0");
  if(!/[1-9]/.test(normal))throw Error("O valor deve ser maior que zero.");return normal;
 }
 async function formulario(){
  const f=document.getElementById("form-transferencia");if(!f)return;
  const anterior=ler(RASCUNHO);if(anterior?.incerta){location.replace("confirmacao.html");return;}
  const select=document.getElementById("conta-origem"),botao=f.querySelector('[type="submit"]');botao.disabled=true;
  try {
   const contas=await api("/api/contas/minhas");if(!Array.isArray(contas)||!contas.length)throw Error("Nenhuma conta vinculada ao usuário.");
   select.replaceChildren();for(const c of contas){const o=document.createElement("option");o.value=String(c.id);o.textContent=`Agência ${c.agencia} · Conta ${c.numero} — ${moeda(c.saldo)}`;select.append(o);}
   const salvo=sessionStorage.getItem("safira.contaId");if(contas.some(c=>String(c.id)===salvo))select.value=salvo;
   const atualizar=()=>{const c=contas.find(c=>String(c.id)===select.value);texto("saldo-origem",c?moeda(c.saldo):"—");};select.addEventListener("change",atualizar);atualizar();botao.disabled=false;
   f.addEventListener("submit",e=>{
    e.preventDefault();texto("mensagem-transferencia","");
    try {
     const c=contas.find(c=>String(c.id)===select.value);if(!c)throw Error("Selecione a origem.");
     const valor=quantia(document.getElementById("valor").value);
     const agencia=document.getElementById("agencia-destino").value.trim(),numero=document.getElementById("numero-destino").value.trim();
     if(c.agencia===agencia&&c.numero===numero)throw Error("Escolha uma conta de destino diferente da origem.");
     const r={chave:crypto.randomUUID(),origemRotulo:`Agência ${c.agencia} · Conta ${c.numero}`,incerta:false,
      dados:{contaOrigemId:c.id,agenciaDestino:agencia,numeroDestino:numero,valor,descricao:document.getElementById("descricao").value.trim()}};
     sessionStorage.removeItem(RESULTADO);sessionStorage.setItem(RASCUNHO,JSON.stringify(r));location.assign("confirmacao.html");
    } catch(erro){texto("mensagem-transferencia",erro.message);}
   });
  }catch(erro){texto("mensagem-transferencia",erro.message);}
 }
 function mostrarResultado(r){
  document.getElementById("revisao-transferencia").hidden=true;
  document.getElementById("resultado-transferencia").hidden=false;
  texto("resultado-mensagem",r.mensagem);texto("resultado-id",String(r.transacaoId));
 }
 function confirmacao(){
  const f=document.getElementById("form-confirmacao");if(!f)return;
  const r=ler(RASCUNHO);if(!r){const resultado=ler(RESULTADO);if(resultado){mostrarResultado(resultado);return;}location.replace("transferencia.html");return;}
  if(!r.dados||!r.chave){sessionStorage.removeItem(RASCUNHO);location.replace("transferencia.html");return;}
  texto("resumo-origem",r.origemRotulo);texto("resumo-destino",`Agência ${r.dados.agenciaDestino} · Conta ${r.dados.numeroDestino}`);
  texto("resumo-valor",moeda(r.dados.valor));texto("resumo-descricao",r.dados.descricao||"Sem descrição");
  const b=document.getElementById("confirmar-transferencia"),check=document.getElementById("concordo"),editar=document.getElementById("editar-transferencia");let enviando=false;
  function avisoIncerto(){editar.hidden=true;texto("mensagem-confirmacao","O resultado da tentativa anterior não foi confirmado. Tente novamente nesta tela: a mesma chave será reutilizada para evitar duplicação.");}
  if(r.incerta)avisoIncerto();
  check.addEventListener("change",()=>{b.disabled=enviando||!check.checked;});
  editar.addEventListener("click",e=>{if(enviando||r.incerta){e.preventDefault();return;}sessionStorage.removeItem(RASCUNHO);});
  f.addEventListener("submit",async e=>{
   e.preventDefault();if(enviando||!check.checked)return;enviando=true;b.disabled=true;editar.hidden=true;
   r.incerta=true;sessionStorage.setItem(RASCUNHO,JSON.stringify(r));texto("mensagem-confirmacao","Enviando transferência. Aguarde...");
   try {
    const resposta=await api("/api/transferencias",{method:"POST",headers:{"Content-Type":"application/json","Idempotency-Key":r.chave},body:JSON.stringify(r.dados)});
    if(!resposta||resposta.status!=="CONCLUIDA"||!resposta.transacaoId)throw Error("Resposta inesperada. Consulte o histórico antes de iniciar outra operação.");
    sessionStorage.setItem(RESULTADO,JSON.stringify(resposta));sessionStorage.removeItem(RASCUNHO);mostrarResultado(resposta);
   }catch(erro){
    if(erro.status&&erro.status<500){r.incerta=false;sessionStorage.setItem(RASCUNHO,JSON.stringify(r));editar.hidden=false;texto("mensagem-confirmacao",erro.message);}
    else{avisoIncerto();texto("mensagem-confirmacao",erro.message+" O resultado não foi confirmado. Reenvie por esta tela com a mesma chave ou confira o histórico; não inicie outra operação igual.");}
   }finally{enviando=false;b.disabled=!check.checked;}
  });
 }
 document.addEventListener("DOMContentLoaded",()=>{
  document.querySelectorAll('[data-sair]').forEach(a=>a.addEventListener("click",()=>{sessionStorage.removeItem(RASCUNHO);sessionStorage.removeItem(RESULTADO);}));
  if(!sessionStorage.getItem("safira.token")){sair();return;}formulario();confirmacao();
 });
 // Ao voltar pelo histórico do navegador, revalida rascunho e resultado sem reenviar POST.
 addEventListener("pageshow",e=>{if(e.persisted)location.reload();});
})();
