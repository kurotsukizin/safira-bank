"use strict";
const CHAVE_TOKEN = "safira.token", CHAVE_CONTA = "safira.contaId";
const formatarMoeda = v => new Intl.NumberFormat("pt-BR", {style: "currency", currency: "BRL"}).format(v);
const formatarDataHora = v => {
    const d = new Date(v);
    return !v || Number.isNaN(d.getTime()) ? "—" : new Intl.DateTimeFormat("pt-BR", {
        dateStyle: "short",
        timeStyle: "short"
    }).format(d);
};
const formatarStatus = s => ({CONCLUIDA: "Concluída", PENDENTE: "Pendente", CANCELADA: "Cancelada"}[s] || s || "—");
const definirTexto = (id, t) => {
    const e = document.getElementById(id);
    if (e) e.textContent = t;
};
const limparAutenticacao = () => {
    sessionStorage.removeItem(CHAVE_TOKEN);
    sessionStorage.removeItem(CHAVE_CONTA);
};
const estaNoLogin = () => location.pathname === "/" || location.pathname.endsWith("/index.html");
const voltarAoLogin = () => {
    limparAutenticacao();
    if (!estaNoLogin()) location.replace("/index.html");
};
const consultarApi = async (url, {autenticar = true, ...opcoes} = {}) => {
    const headers = new Headers(opcoes.headers);
    headers.set("Accept", "application/json");
    if (autenticar) {
        const t = sessionStorage.getItem(CHAVE_TOKEN);
        if (!t) {
            voltarAoLogin();
            throw Error("Autenticação necessária.");
        }
        headers.set("Authorization", `Bearer ${t}`);
    }
    const r = await fetch(url, {...opcoes, headers, cache: "no-store", credentials: "omit"});
    const texto = await r.text();
    let dados = null;
    try {
        dados = texto ? JSON.parse(texto) : null;
    } catch {
    }
    if (!r.ok) {
        if (autenticar && r.status === 401) {
            voltarAoLogin();
            throw Error("Sessão expirada. Entre novamente.");
        }
        throw Error(!autenticar && r.status === 401 ? "E-mail ou senha inválidos, ou usuário sem acesso ativo." : dados?.mensagem || dados?.detail || `Não foi possível concluir a consulta (HTTP ${r.status}).`);
    }
    return dados;
};
const configurarLogin = () => {
    const f = document.getElementById("form-login");
    if (!f) return;
    limparAutenticacao();
    const senha = document.getElementById("senha"), alternar = document.getElementById("alternar-senha");
    alternar?.addEventListener("click", () => {
        const mostrar = senha.type === "password";
        senha.type = mostrar ? "text" : "password";
        alternar.textContent = mostrar ? "Ocultar" : "Mostrar";
        alternar.setAttribute("aria-pressed", String(mostrar));
        alternar.setAttribute("aria-label", mostrar ? "Ocultar senha" : "Mostrar senha");
    });
    f.addEventListener("submit", async e => {
        e.preventDefault();
        const b = f.querySelector('button[type="submit"]');
        b.disabled = true;
        definirTexto("mensagem-login", "Validando acesso...");
        try {
            limparAutenticacao();
            const d = await consultarApi("/api/auth/login", {
                autenticar: false,
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({email: document.getElementById("email").value.trim(), senha: senha.value})
            });
            if (!d?.accessToken) throw Error("A API não retornou um token de acesso.");
            sessionStorage.setItem(CHAVE_TOKEN, d.accessToken);
            senha.value = "";
            location.replace("inicio.html");
        } catch (erro) {
            definirTexto("mensagem-login", erro.message || "Não foi possível realizar o login.");
        } finally {
            b.disabled = false;
        }
    });
};
const configurarSaida = () => document.querySelectorAll('[data-sair]').forEach(a => a.addEventListener("click", () => limparAutenticacao()));
const criarCelula = t => {
    const e = document.createElement("td");
    e.textContent = t;
    return e;
};
const mostrarMensagemHistorico = m => {
    const corpo = document.getElementById("historico");
    if (!corpo) return;
    const l = document.createElement("tr"), c = criarCelula(m);
    c.colSpan = 4;
    l.append(c);
    corpo.replaceChildren(l);
};
const preencherResumo = c => {
    definirTexto("titulo-inicio", `Olá, ${c.titular}`);
    definirTexto("saldo-conta", formatarMoeda(c.saldo));
    definirTexto("dados-conta", `Agência ${c.agencia} · Conta ${c.numero}`);
    definirTexto("status-conta", "Saldo consultado na API para a conta selecionada.");
};
const carregarHistorico = async id => {
    const corpo = document.getElementById("historico");
    if (!corpo) return;
    mostrarMensagemHistorico("Carregando movimentações...");
    try {
        const lista = await consultarApi(`/api/lancamentos?contaId=${encodeURIComponent(id)}`);
        if (!Array.isArray(lista)) throw Error("O histórico não retornou uma lista.");
        if (!lista.length) {
            mostrarMensagemHistorico("Nenhum lançamento encontrado para esta conta.");
            return;
        }
        const fragmento = document.createDocumentFragment();
        for (const l of lista) {
            const tr = document.createElement("tr");
            tr.append(criarCelula(formatarDataHora(l.criadoEm)));
            const desc = criarCelula(""), principal = document.createElement("span"),
                detalhe = document.createElement("span");
            principal.className = "descricao-principal";
            principal.textContent = l.descricao || "Lançamento financeiro";
            detalhe.className = "descricao-detalhe";
            detalhe.textContent = l.tipo === "CREDITO" ? "Crédito na conta" : l.tipo === "DEBITO" ? "Débito na conta" : "Movimentação da conta";
            desc.append(principal, detalhe);
            tr.append(desc);
            const valor = criarCelula((l.tipo === "CREDITO" ? "+ " : l.tipo === "DEBITO" ? "− " : "") + formatarMoeda(l.valor));
            valor.className = l.tipo === "CREDITO" ? "valor-entrada" : l.tipo === "DEBITO" ? "valor-saida" : "valor-neutro";
            tr.append(valor);
            const c = criarCelula(""), s = document.createElement("span");
            s.className = "status";
            if (l.status === "CONCLUIDA") s.classList.add("status-concluida");
            if (l.status === "CANCELADA") s.classList.add("status-cancelada");
            s.textContent = formatarStatus(l.status);
            c.append(s);
            tr.append(c);
            fragmento.append(tr);
        }
        corpo.replaceChildren(fragmento);
    } catch (e) {
        mostrarMensagemHistorico(e.message);
    }
};
const selecionarConta = async c => {
    sessionStorage.setItem(CHAVE_CONTA, String(c.id));
    preencherResumo(c);
    await carregarHistorico(c.id);
};
const criarSeletorContas = (contas, atual) => {
    document.getElementById("seletor-contas")?.remove();
    if (contas.length < 2) return;
    const campo = document.createElement("div"), label = document.createElement("label"),
        select = document.createElement("select");
    campo.id = "seletor-contas";
    campo.className = "campo";
    label.htmlFor = "conta-selecionada";
    label.textContent = "Conta para consulta";
    select.id = "conta-selecionada";
    contas.forEach(c => {
        const o = document.createElement("option");
        o.value = String(c.id);
        o.textContent = `Agência ${c.agencia} · Conta ${c.numero}`;
        select.append(o);
    });
    select.value = String(atual.id);
    select.addEventListener("change", async () => {
        const c = contas.find(c => String(c.id) === select.value);
        if (!c) return;
        select.disabled = true;
        try {
            await selecionarConta(c);
        } finally {
            select.disabled = false;
        }
    });
    campo.append(label, select);
    document.getElementById("dados-conta")?.insertAdjacentElement("afterend", campo);
};
const carregarAreaAutenticada = async () => {
    if (!sessionStorage.getItem(CHAVE_TOKEN)) {
        voltarAoLogin();
        return;
    }
    try {
        const contas = await consultarApi("/api/contas/minhas");
        if (!Array.isArray(contas)) throw Error("A API não retornou uma lista de contas.");
        if (!contas.length) {
            sessionStorage.removeItem(CHAVE_CONTA);
            definirTexto("titulo-inicio", "Minha conta");
            definirTexto("saldo-conta", "—");
            definirTexto("dados-conta", "Nenhuma conta vinculada.");
            definirTexto("status-conta", "Usuário sem conta cadastrada.");
            mostrarMensagemHistorico("Nenhuma conta vinculada ao usuário.");
            return;
        }
        const atual = contas.find(c => String(c.id) === sessionStorage.getItem(CHAVE_CONTA)) || contas[0];
        criarSeletorContas(contas, atual);
        await selecionarConta(atual);
    } catch (e) {
        definirTexto("titulo-inicio", "Minha conta");
        definirTexto("saldo-conta", "Indisponível");
        definirTexto("dados-conta", "Não foi possível consultar a conta.");
        definirTexto("status-conta", e.message);
        mostrarMensagemHistorico(e.message);
    }
};
document.addEventListener("DOMContentLoaded", () => {
    configurarSaida();
    if (estaNoLogin()) {
        configurarLogin();
        return;
    }
    if (!sessionStorage.getItem(CHAVE_TOKEN)) {
        voltarAoLogin();
        return;
    }
    if (document.getElementById("historico") || document.getElementById("titulo-inicio")) carregarAreaAutenticada();
});