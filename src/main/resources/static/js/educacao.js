(() => {
    "use strict";

    const PERGUNTAS = [
        {
            texto: "O que é uma conta laranja?",
            opcoes: [
                "Uma conta com tarifa promocional.",
                "Uma conta usada para movimentar dinheiro de origem ilícita em nome de quem não é o verdadeiro beneficiário.",
                "Uma conta exclusiva para menores de idade.",
                "Uma conta que só recebe transferências."
            ],
            correta: 1,
            explicacao: "Em uma conta laranja, o titular aparece no cadastro, mas o dinheiro pertence a outra pessoa que quer ocultar a sua identidade."
        },
        {
            texto: "Alguém oferece dinheiro para você emprestar a sua conta por alguns dias. O que fazer?",
            opcoes: [
                "Aceitar, se o valor for pequeno.",
                "Aceitar, se for um amigo.",
                "Recusar e comunicar o banco pelos canais oficiais.",
                "Aceitar, mas só emprestar o número da conta."
            ],
            correta: 2,
            explicacao: "Ceder a conta, ou dados dela, pode envolver você em fraudes e outros crimes. Recuse e avise o banco."
        },
        {
            texto: "Qual é um risco de ceder a sua conta ou seus documentos para terceiros?",
            opcoes: [
                "Nenhum, porque o dinheiro não é seu.",
                "Ficar envolvido em crimes como estelionato e lavagem de dinheiro.",
                "Receber mais cartões de crédito.",
                "Aumentar o limite da conta."
            ],
            correta: 1,
            explicacao: "Mesmo sem saber de tudo, o titular pode ser investigado e ter a conta bloqueada ou encerrada."
        },
        {
            texto: "Uma pessoa desconhecida pede o código de segurança que você recebeu por SMS. O que você faz?",
            opcoes: [
                "Envia, se disser que é do banco.",
                "Envia só os 3 primeiros dígitos.",
                "Não compartilha o código com ninguém e contata o banco por um canal oficial.",
                "Envia, se a conversa for pelo WhatsApp."
            ],
            correta: 2,
            explicacao: "Códigos, senhas e biometria são pessoais. Instituições financeiras não pedem esses dados por aplicativos de mensagem."
        },
        {
            texto: "Qual situação é um sinal de alerta?",
            opcoes: [
                "Receber o salário na data combinada.",
                "Receber uma transferência de origem desconhecida e ser pressionado a repassar o valor.",
                "Consultar o extrato no aplicativo oficial.",
                "Cadastrar uma chave Pix pelo aplicativo do banco."
            ],
            correta: 1,
            explicacao: "Valores de origem desconhecida seguidos de pedido de repasse são um padrão comum de fraude. Pare e fale com o banco."
        },
        {
            texto: "Recebeu uma proposta de dinheiro fácil em troca de receber e repassar valores. O que isso indica?",
            opcoes: [
                "Uma oportunidade de renda extra segura.",
                "Um possível esquema de conta laranja.",
                "Um benefício do banco.",
                "Uma promoção para novos clientes."
            ],
            correta: 1,
            explicacao: "Ofertas de ganho fácil pelo uso da sua conta são uma forma comum de aliciamento. Não responda e denuncie."
        },
        {
            texto: "Um desconhecido pede uma foto sua segurando o documento. O que fazer?",
            opcoes: [
                "Enviar, se ele prometer apagar depois.",
                "Enviar, se a oferta parecer boa.",
                "Não enviar. A imagem pode ser usada para abrir contas em seu nome.",
                "Enviar apenas a foto do documento, sem o rosto."
            ],
            correta: 2,
            explicacao: "Fotos de rosto e documentos podem ser usadas para fraudar cadastros. Compartilhe só com serviços oficiais e confiáveis."
        },
        {
            texto: "Você suspeita que seus dados foram usados indevidamente. Qual atitude é adequada?",
            opcoes: [
                "Ignorar, porque pode passar.",
                "Avisar o banco, registrar boletim de ocorrência e conferir os vínculos em seu nome, como no Registrato do Banco Central.",
                "Apagar as mensagens.",
                "Publicar os seus dados nas redes sociais."
            ],
            correta: 1,
            explicacao: "Agir rápido ajuda a limitar o problema e a comprovar que você é vítima."
        }
    ];

    let indice = 0;
    let acertos = 0;
    let respondida = false;

    const area = () => document.getElementById("quiz-area");

    const el = (tag, props = {}, filhos = []) => {
        const e = document.createElement(tag);
        Object.entries(props).forEach(([k, v]) => {
            if (k === "text") e.textContent = v;
            else if (k === "class") e.className = v;
            else e.setAttribute(k, v);
        });
        filhos.forEach((f) => e.appendChild(f));
        return e;
    };

    const renderPergunta = () => {
        const p = PERGUNTAS[indice];
        respondida = false;
        const raiz = area();
        raiz.replaceChildren();

        raiz.appendChild(el("p", {
            class: "edu-progresso",
            text: `Pergunta ${indice + 1} de ${PERGUNTAS.length}`
        }));

        const form = el("form", { class: "edu-form" });
        const grupo = el("fieldset", { class: "edu-grupo" });
        const legenda = el("legend", { text: p.texto });
        grupo.appendChild(legenda);

        p.opcoes.forEach((texto, i) => {
            const id = `opcao-${indice}-${i}`;
            const input = el("input", { type: "radio", name: "resposta", id, value: String(i) });
            const rotulo = el("label", { for: id, class: "edu-opcao" }, [input, el("span", { text: texto })]);
            grupo.appendChild(rotulo);
        });

        const aviso = el("p", { class: "edu-erro", role: "alert", id: "quiz-erro" });
        const botao = el("button", { type: "submit", class: "botao botao-primario", text: "Responder" });
        const feedback = el("div", { id: "quiz-feedback", role: "status", "aria-live": "polite", class: "edu-feedback" });

        form.append(grupo, aviso, botao);
        form.addEventListener("submit", (ev) => {
            ev.preventDefault();
            if (respondida) return;
            const marcada = form.querySelector("input[name=resposta]:checked");
            if (!marcada) {
                aviso.textContent = "Escolha uma alternativa antes de responder.";
                return;
            }
            aviso.textContent = "";
            responder(Number(marcada.value), form, botao, feedback);
        });

        raiz.append(form, feedback);
        legenda.setAttribute("tabindex", "-1");
        legenda.focus();
    };

    const responder = (escolha, form, botao, feedback) => {
        const p = PERGUNTAS[indice];
        const acertou = escolha === p.correta;
        respondida = true;
        if (acertou) acertos += 1;

        form.querySelectorAll("input").forEach((i) => { i.disabled = true; });
        botao.hidden = true;

        const rotulos = form.querySelectorAll(".edu-opcao");
        rotulos[p.correta].classList.add("edu-certa");
        if (!acertou) rotulos[escolha].classList.add("edu-errada");

        feedback.replaceChildren(
            el("p", { class: "edu-resultado", text: acertou ? "Resposta correta." : "Resposta incorreta." }),
            el("p", { text: `Alternativa correta: ${p.opcoes[p.correta]}` }),
            el("p", { text: p.explicacao })
        );

        const ultima = indice === PERGUNTAS.length - 1;
        const proximo = el("button", {
            type: "button",
            class: "botao botao-primario",
            text: ultima ? "Ver resultado" : "Próxima pergunta"
        });
        proximo.addEventListener("click", () => {
            if (ultima) renderResultado();
            else { indice += 1; renderPergunta(); }
        });
        feedback.appendChild(proximo);
    };

    const renderResultado = () => {
        const raiz = area();
        raiz.replaceChildren();
        const total = PERGUNTAS.length;
        const msg = acertos === total
            ? "Excelente! Você reconheceu todos os riscos."
            : acertos >= Math.ceil(total * 0.6)
                ? "Bom resultado. Revise as explicações das perguntas que errou."
                : "Vale rever o conteúdo e assistir aos vídeos antes de tentar novamente.";

        const titulo = el("h3", { text: `Você acertou ${acertos} de ${total}.`, tabindex: "-1", id: "quiz-resultado" });
        const refazer = el("button", { type: "button", class: "botao botao-primario", text: "Refazer o quiz" });
        refazer.addEventListener("click", () => { indice = 0; acertos = 0; renderPergunta(); });
        raiz.append(titulo, el("p", { text: msg }), refazer);
        titulo.focus();
    };

    const renderVideos = () => {
        const raiz = document.getElementById("videos-area");
        raiz.replaceChildren();
        const lista = typeof VIDEOS_EDUCATIVOS !== "undefined" ? VIDEOS_EDUCATIVOS : [];

        if (!lista.length) {
            raiz.appendChild(el("p", { class: "edu-nota", text: "Nenhum vídeo cadastrado ainda. Adicione itens em js/videos.js." }));
            return;
        }

        lista.forEach((v, i) => {
            const cartao = el("article", { class: "edu-video" });
            cartao.appendChild(el("h3", { text: v.titulo }));
            cartao.appendChild(el("p", { text: v.descricao }));
            if (v.fonte) cartao.appendChild(el("p", { class: "edu-fonte", text: `Fonte: ${v.fonte}` }));

            if (v.youtubeId) {
                const quadro = el("div", { class: "edu-quadro" }, [
                    el("iframe", {
                        src: `https://www.youtube-nocookie.com/embed/${encodeURIComponent(v.youtubeId)}`,
                        title: `Vídeo: ${v.titulo}`,
                        loading: "lazy",
                        allow: "encrypted-media; picture-in-picture",
                        allowfullscreen: ""
                    })
                ]);
                cartao.appendChild(quadro);
                cartao.appendChild(el("a", {
                    class: "link-texto",
                    href: `https://www.youtube.com/watch?v=${encodeURIComponent(v.youtubeId)}`,
                    target: "_blank",
                    rel: "noopener noreferrer",
                    text: "Abrir no YouTube (nova aba)"
                }));
            }

            if (v.transcricao) {
                cartao.appendChild(el("details", { class: "edu-item" }, [
                    el("summary", { text: "Ler transcrição" }),
                    el("p", { text: v.transcricao })
                ]));
            }
            raiz.appendChild(cartao);
        });
    };

    const iniciarAbas = () => {
        const abas = Array.from(document.querySelectorAll("[role=tab]"));
        const painel = (aba) => document.getElementById(aba.getAttribute("aria-controls"));

        const ativar = (aba, foco = true) => {
            abas.forEach((a) => {
                const ativa = a === aba;
                a.setAttribute("aria-selected", String(ativa));
                a.tabIndex = ativa ? 0 : -1;
                painel(a).hidden = !ativa;
            });
            if (foco) aba.focus();
        };

        abas.forEach((aba, i) => {
            aba.addEventListener("click", () => ativar(aba, false));
            aba.addEventListener("keydown", (ev) => {
                let alvo = null;
                if (ev.key === "ArrowRight") alvo = abas[(i + 1) % abas.length];
                if (ev.key === "ArrowLeft") alvo = abas[(i - 1 + abas.length) % abas.length];
                if (ev.key === "Home") alvo = abas[0];
                if (ev.key === "End") alvo = abas[abas.length - 1];
                if (alvo) { ev.preventDefault(); ativar(alvo); }
            });
        });

        if (location.hash === "#videos") ativar(document.getElementById("aba-videos"), false);
    };

    document.addEventListener("DOMContentLoaded", () => {
        iniciarAbas();
        renderPergunta();
        renderVideos();
        document.getElementById("quiz-area").querySelector("legend")?.removeAttribute("tabindex");
    });
})();
