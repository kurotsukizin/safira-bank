const CHAVE_TOKEN = "safira.token";
const CHAVE_CONTA = "safira.contaId";

const formatarMoeda = (valor) =>
    new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: "BRL"
    }).format(valor);

const formatarDataHora = (valor) => {
    if (!valor) return "—";

    const data = new Date(valor);

    if (Number.isNaN(data.getTime())) return "—";

    return new Intl.DateTimeFormat("pt-BR", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    }).format(data);
};

const formatarStatus = (status) => ({
    CONCLUIDA: "Concluída",
    PENDENTE: "Pendente",
    CANCELADA: "Cancelada"
}[status] || status || "—");

const definirTexto = (id, texto) => {
    const elemento = document.getElementById(id);

    if (elemento) {
        elemento.textContent = texto;
    }
};

const limparAutenticacao = () => {
    sessionStorage.removeItem(CHAVE_TOKEN);
    sessionStorage.removeItem(CHAVE_CONTA);
};

const voltarAoLogin = () => {
    limparAutenticacao();

    const caminho = window.location.pathname;

    const estaNoLogin =
        caminho === "/" ||
        caminho.endsWith("/index.html");

    if (!estaNoLogin) {
        window.location.replace("/index.html");
    }
};
const consultarApi = async (
    url,
    {autenticar = true, ...opcoes} = {}
) => {
    const headers = new Headers(opcoes.headers);
    headers.set("Accept", "application/json");

    if (autenticar) {
        const token = sessionStorage.getItem(CHAVE_TOKEN);

        if (!token) {
            voltarAoLogin();
            throw new Error("Autenticação necessária.");
        }

        headers.set("Authorization", `Bearer ${token}`);
    }

    const resposta = await fetch(url, {
        ...opcoes,
        headers,
        cache: "no-store",
        credentials: "omit"
    });

    const texto = await resposta.text();
    let dados = null;

    if (texto) {
        try {
            dados = JSON.parse(texto);
        } catch {
            dados = null;
        }
    }

    if (!resposta.ok) {
        if (autenticar && resposta.status === 401) {
            voltarAoLogin();
            throw new Error("Sessão expirada. Entre novamente.");
        }

        const mensagem = !autenticar && resposta.status === 401
            ? "E-mail ou senha inválidos, ou usuário sem acesso ativo."
            : dados?.mensagem
            || dados?.detail
            || `Não foi possível concluir a consulta (HTTP ${resposta.status}).`;

        throw new Error(mensagem);
    }

    return dados;
};

const configurarLogin = () => {
    const formulario = document.getElementById("form-login");

    if (!formulario) return;

    // Voltar ao login encerra a autenticação local anterior.
    limparAutenticacao();

    const campoSenha = document.getElementById("senha");
    const alternar = document.getElementById("alternar-senha");

    alternar?.addEventListener("click", () => {
        const mostrar = campoSenha.type === "password";

        campoSenha.type = mostrar ? "text" : "password";
        alternar.textContent = mostrar ? "Ocultar" : "Mostrar";
        alternar.setAttribute("aria-pressed", String(mostrar));
        alternar.setAttribute(
            "aria-label",
            mostrar ? "Ocultar senha" : "Mostrar senha"
        );
    });

    formulario.addEventListener("submit", async (evento) => {
        evento.preventDefault();

        const botao = formulario.querySelector(
            'button[type="submit"]'
        );

        botao.disabled = true;
        definirTexto("mensagem-login", "Validando acesso...");

        try {
            limparAutenticacao();

            const dados = await consultarApi("/api/auth/login", {
                autenticar: false,
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    email: document.getElementById("email").value.trim(),
                    senha: campoSenha.value
                })
            });

            if (!dados?.accessToken) {
                throw new Error("A API não retornou um token de acesso.");
            }

            sessionStorage.setItem(
                CHAVE_TOKEN,
                dados.accessToken
            );

            campoSenha.value = "";
            window.location.replace("inicio.html");
        } catch (erro) {
            definirTexto(
                "mensagem-login",
                erro.message || "Não foi possível realizar o login."
            );
        } finally {
            botao.disabled = false;
        }
    });
};

const configurarSaida = () => {
    document.querySelectorAll('a[href="index.html"]')
        .forEach((link) => {
            link.addEventListener("click", () => {
                limparAutenticacao();
            });
        });
};

const criarCelula = (texto) => {
    const celula = document.createElement("td");
    celula.textContent = texto;
    return celula;
};

const mostrarMensagemHistorico = (mensagem) => {
    const corpo = document.getElementById("historico");

    if (!corpo) return;

    const linha = document.createElement("tr");
    const coluna = criarCelula(mensagem);

    coluna.colSpan = 4;
    linha.appendChild(coluna);
    corpo.replaceChildren(linha);
};

const preencherResumo = (conta) => {
    definirTexto("titulo-inicio", `Olá, ${conta.titular}`);
    definirTexto("saldo-conta", formatarMoeda(conta.saldo));

    definirTexto(
        "dados-conta",
        `Agência ${conta.agencia} · Conta ${conta.numero}`
    );

    definirTexto(
        "status-conta",
        "Saldo consultado na API para a conta selecionada."
    );
};

const carregarHistorico = async (contaId) => {
    const corpo = document.getElementById("historico");

    if (!corpo) return;

    mostrarMensagemHistorico("Carregando movimentações...");

    try {
        const lancamentos = await consultarApi(
            `/api/lancamentos?contaId=${encodeURIComponent(contaId)}`
        );

        if (!Array.isArray(lancamentos)) {
            throw new Error("O histórico não retornou uma lista.");
        }

        if (lancamentos.length === 0) {
            mostrarMensagemHistorico(
                "Nenhum lançamento encontrado para esta conta."
            );
            return;
        }

        const linhas = document.createDocumentFragment();

        lancamentos.forEach((lancamento) => {
            const linha = document.createElement("tr");

            linha.appendChild(
                criarCelula(formatarDataHora(lancamento.criadoEm))
            );

            const descricao = criarCelula("");
            const principal = document.createElement("span");
            const detalhe = document.createElement("span");

            principal.className = "descricao-principal";
            principal.textContent =
                lancamento.descricao || "Lançamento financeiro";

            detalhe.className = "descricao-detalhe";
            detalhe.textContent = lancamento.tipo === "CREDITO"
                ? "Crédito na conta"
                : lancamento.tipo === "DEBITO"
                    ? "Débito na conta"
                    : "Movimentação da conta";

            descricao.append(principal, detalhe);
            linha.appendChild(descricao);

            const valor = criarCelula("");
            const credito = lancamento.tipo === "CREDITO";
            const debito = lancamento.tipo === "DEBITO";

            valor.className = credito
                ? "valor-entrada"
                : debito
                    ? "valor-saida"
                    : "valor-neutro";

            const sinal = credito ? "+ " : debito ? "− " : "";

            valor.textContent =
                sinal + formatarMoeda(lancamento.valor);

            linha.appendChild(valor);

            const celulaStatus = criarCelula("");
            const status = document.createElement("span");

            status.classList.add("status");
            status.textContent = formatarStatus(lancamento.status);

            if (lancamento.status === "CONCLUIDA") {
                status.classList.add("status-concluida");
            } else if (lancamento.status === "CANCELADA") {
                status.classList.add("status-cancelada");
            }

            celulaStatus.appendChild(status);
            linha.appendChild(celulaStatus);
            linhas.appendChild(linha);
        });

        corpo.replaceChildren(linhas);
    } catch (erro) {
        mostrarMensagemHistorico(erro.message);
    }
};

const selecionarConta = async (conta) => {
    sessionStorage.setItem(CHAVE_CONTA, String(conta.id));
    preencherResumo(conta);
    await carregarHistorico(conta.id);
};

const criarSeletorContas = (contas, contaAtual) => {
    if (contas.length < 2) return;

    const campo = document.createElement("div");
    campo.className = "campo";

    const label = document.createElement("label");
    label.htmlFor = "conta-selecionada";
    label.textContent = "Conta para consulta";

    const select = document.createElement("select");
    select.id = "conta-selecionada";

    contas.forEach((conta) => {
        const option = document.createElement("option");

        option.value = String(conta.id);
        option.textContent =
            `Agência ${conta.agencia} · Conta ${conta.numero}`;

        select.appendChild(option);
    });

    select.value = String(contaAtual.id);

    select.addEventListener("change", async () => {
        const conta = contas.find(
            (item) => String(item.id) === select.value
        );

        if (!conta) return;

        select.disabled = true;

        try {
            await selecionarConta(conta);
        } finally {
            select.disabled = false;
        }
    });

    campo.append(label, select);

    const referencia = document.getElementById("dados-conta");

    if (referencia) {
        referencia.insertAdjacentElement("afterend", campo);
    } else {
        document.getElementById("conteudo")?.prepend(campo);
    }
};

const carregarAreaAutenticada = async () => {
    if (!sessionStorage.getItem(CHAVE_TOKEN)) {
        voltarAoLogin();
        return;
    }

    try {
        const contas = await consultarApi("/api/contas/minhas");

        if (!Array.isArray(contas)) {
            throw new Error("A API não retornou uma lista de contas.");
        }

        if (contas.length === 0) {
            sessionStorage.removeItem(CHAVE_CONTA);

            definirTexto("titulo-inicio", "Minha conta");
            definirTexto("saldo-conta", "—");
            definirTexto("dados-conta", "Nenhuma conta vinculada.");
            definirTexto("status-conta", "Usuário sem conta cadastrada.");

            mostrarMensagemHistorico(
                "Nenhuma conta vinculada ao usuário."
            );
            return;
        }

        const idSalvo = sessionStorage.getItem(CHAVE_CONTA);

        const contaAtual = contas.find(
            (conta) => String(conta.id) === idSalvo
        ) || contas[0];

        criarSeletorContas(contas, contaAtual);
        await selecionarConta(contaAtual);
    } catch (erro) {
        definirTexto("titulo-inicio", "Minha conta");
        definirTexto("saldo-conta", "Indisponível");
        definirTexto("dados-conta", "Não foi possível consultar a conta.");
        definirTexto("status-conta", erro.message);
        mostrarMensagemHistorico(erro.message);
    }
};

document.addEventListener("DOMContentLoaded", () => {
    configurarSaida();

    const caminho = window.location.pathname;

    const estaNoLogin =
        caminho === "/" ||
        caminho.endsWith("/index.html");

    if (estaNoLogin) {
        if (document.getElementById("form-login")) {
            configurarLogin();
        }

        return;
    }

    const paginaConta =
        document.getElementById("titulo-inicio") ||
        document.getElementById("historico");

    if (paginaConta) {
        carregarAreaAutenticada();
    }
});