# -*- coding: utf-8 -*-
"""Desenha as figuras do DR4-AT, em Pillow.

    python scripts/gerar-diagramas.py

Gera docs/evidencias/fig-01-arquitetura.png e fig-02-caminho-do-evento.png.

Mesma convenção visual do TP2 e do TP3: contorno escuro sobre preenchimento claro, legível
impresso em escala de cinza. Cor nunca é o único código: a forma e o rótulo dizem a mesma coisa.
"""

import math
import os

from PIL import Image, ImageDraw, ImageFont

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DESTINO = os.path.join(RAIZ, 'docs', 'evidencias')

FUNDO = (255, 255, 255)
TINTA = (24, 24, 32)
TRACO = (70, 70, 90)
CINZA = (110, 110, 125)
VERMELHO = (170, 45, 45)
AZUL = (14, 116, 190)
VERDE = (22, 120, 70)

SERVICO = (235, 245, 255)
AUXILIAR = (236, 250, 241)
BROKER = (243, 240, 255)
INFRA = (245, 245, 248)
OBS = (255, 246, 232)
FALHA = (253, 236, 236)

F = 'C:/Windows/Fonts/'
titulo = ImageFont.truetype(F + 'segoeuib.ttf', 40)
rotulo = ImageFont.truetype(F + 'segoeuib.ttf', 27)
corpo = ImageFont.truetype(F + 'segoeui.ttf', 22)
miudo = ImageFont.truetype(F + 'segoeui.ttf', 20)


class Tela:
    def __init__(self, largura, altura):
        self.img = Image.new('RGB', (largura, altura), FUNDO)
        self.d = ImageDraw.Draw(self.img)
        self.L, self.A = largura, altura

    def centrado(self, texto, fonte, x, y, cor=TINTA):
        w = self.d.textbbox((0, 0), texto, font=fonte)[2]
        self.d.text((x - w / 2, y), texto, font=fonte, fill=cor)

    def caixa(self, x, y, larg, alt, titulo_txt, linhas=(), fill=SERVICO, borda=TRACO, tracejada=False):
        xy = [x, y, x + larg, y + alt]
        self.d.rounded_rectangle(xy, radius=14, fill=fill, outline=None if tracejada else borda, width=3)
        if tracejada:
            self._tracejado(xy, 14, borda)
        cy = y + 16 if linhas else y + (alt - 34) / 2
        self.centrado(titulo_txt, rotulo, x + larg / 2, cy)
        for i, l in enumerate(linhas):
            self.centrado(l, miudo, x + larg / 2, cy + 40 + i * 27, CINZA)
        return x, y, larg, alt

    def _tracejado(self, xy, raio, cor, passo=14):
        x0, y0, x1, y1 = xy
        for x in range(int(x0) + raio, int(x1) - raio, passo * 2):
            self.d.line([x, y0, min(x + passo, x1 - raio), y0], fill=cor, width=3)
            self.d.line([x, y1, min(x + passo, x1 - raio), y1], fill=cor, width=3)
        for y in range(int(y0) + raio, int(y1) - raio, passo * 2):
            self.d.line([x0, y, x0, min(y + passo, y1 - raio)], fill=cor, width=3)
            self.d.line([x1, y, x1, min(y + passo, y1 - raio)], fill=cor, width=3)

    def seta(self, p0, p1, cor=TRACO, largura=3, tracejada=False, dupla=False):
        if tracejada:
            dx, dy = p1[0] - p0[0], p1[1] - p0[1]
            n = max(int(math.hypot(dx, dy) / 13), 1)
            for i in range(0, n, 2):
                self.d.line([(p0[0] + dx * i / n, p0[1] + dy * i / n),
                             (p0[0] + dx * (i + 1) / n, p0[1] + dy * (i + 1) / n)], fill=cor, width=largura)
        else:
            self.d.line([p0, p1], fill=cor, width=largura)
        self._ponta(p0, p1, cor)
        if dupla:
            self._ponta(p1, p0, cor)

    def _ponta(self, p0, p1, cor):
        ang = math.atan2(p1[1] - p0[1], p1[0] - p0[0])
        c = 16
        self.d.polygon([p1,
                        (p1[0] - c * math.cos(ang - 0.4), p1[1] - c * math.sin(ang - 0.4)),
                        (p1[0] - c * math.cos(ang + 0.4), p1[1] - c * math.sin(ang + 0.4))], fill=cor)

    def etiqueta(self, texto, x, y, fonte=miudo, cor=TRACO):
        w, h = self.d.textbbox((0, 0), texto, font=fonte)[2:]
        self.d.rectangle([x - w / 2 - 6, y - 3, x + w / 2 + 6, y + h + 3], fill=FUNDO)
        self.d.text((x - w / 2, y), texto, font=fonte, fill=cor)

    def salvar(self, nome):
        caminho = os.path.join(DESTINO, nome)
        self.img.save(caminho, 'PNG')
        print('gerado: %s (%dx%d)' % (caminho, self.L, self.A))


def arquitetura():
    """Síncrono em cima (a Saga), assíncrono no meio (os eventos), observabilidade na base."""
    t = Tela(2000, 1420)
    t.centrado('CinePass orientado a eventos: serviços, Kafka e observabilidade', titulo, 1000, 22)

    t.caixa(330, 100, 300, 70, 'Eureka :8761', fill=INFRA, tracejada=True)
    t.etiqueta('todos os serviços se registram', 480, 180, cor=CINZA)
    t.caixa(40, 255, 210, 100, 'Cliente', ['HTTP'], fill=INFRA)
    t.caixa(330, 220, 300, 170, 'API Gateway :8080', ['gera X-Correlation-Id', 'inicia o trace', 'roteia via Eureka'], fill=INFRA)
    t.seta((250, 305), (330, 305))

    t.caixa(760, 190, 480, 250, 'reserva-service :8081',
            ['Saga orquestrada (estado persistido)', 'cinepass_db: reservas e sessões', 'outbox_mensagens na mesma transação', 'relay do outbox publica no Kafka'])
    t.seta((630, 305), (760, 305))

    t.caixa(1370, 180, 320, 110, 'pagamento-service', [':8082 · pagamento_db'])
    t.caixa(1370, 340, 320, 110, 'ingresso-service', [':8083 · ingresso_db'])
    t.seta((1240, 250), (1370, 235), dupla=True)
    t.seta((1240, 380), (1370, 395), dupla=True)
    t.etiqueta('HTTP', 1305, 302, cor=CINZA)
    t.caixa(1730, 180, 240, 270, 'Saga', ['participantes', 'por HTTP;', 'falha no ingresso', 'estorna o pagamento', 'e cancela a reserva'], fill=FALHA, borda=VERMELHO, tracejada=True)

    t.caixa(660, 540, 680, 180, 'Apache Kafka 4.3 (KRaft)',
            ['cinepass.reserva.eventos', '3 partições · chave = reservaId', 'ordem por reserva, paralelismo entre reservas'], fill=BROKER)
    t.seta((1000, 440), (1000, 540))
    t.etiqueta('eventos pelo outbox (+ traceparent, correlationId)', 1000, 475)

    consumidores = [(90, 'notificacao-service :8084', ['notificacao_db', 'inbox + notificações']),
                    (560, 'reputacao-service :8085', ['reputacao_db', 'inbox + reputação do cliente']),
                    (1030, 'auditoria-service :8086', ['auditoria_db', 'inbox + trilha de eventos'])]
    for x, nome, linhas in consumidores:
        t.caixa(x, 860, 400, 150, nome, linhas, fill=AUXILIAR)
        t.seta((1000, 720), (x + 200, 860))
    t.etiqueta('cada serviço no seu grupo · 3 threads · idempotente pelo eventId', 760, 790)

    t.caixa(1520, 540, 440, 180, 'DLT por consumidor', ['<tópico>.<consumidor>.DLT', 'mesma partição de origem', 'POST .../dlt/reprocessar'], fill=FALHA, borda=VERMELHO, tracejada=True)
    t.seta((1430, 870), (1620, 720), cor=VERMELHO, tracejada=True)
    t.etiqueta('após 3 tentativas', 1640, 790, cor=VERMELHO)

    t.d.rounded_rectangle([40, 1080, 1960, 1380], radius=16, fill=OBS, outline=TRACO, width=3)
    t.centrado('Observabilidade (todos os serviços)', rotulo, 1000, 1095)
    t.caixa(80, 1150, 820, 200, 'Logs centralizados (ELK 9.5)',
            ['Logback JSON → Logstash :5000 → Elasticsearch → Kibana :5601', 'campos: service, correlationId, reservaId,', 'eventId, eventType, traceId, spanId'], fill=FUNDO)
    t.caixa(940, 1150, 560, 200, 'Rastreamento (Zipkin :9411)',
            ['Micrometer Tracing + Brave', 'HTTP, outbox e Kafka no mesmo trace', '(traceparent W3C)'], fill=FUNDO)
    t.caixa(1540, 1150, 380, 200, 'Kafka UI :8090', ['tópicos, mensagens,', 'grupos e lag'], fill=FUNDO)
    t.salvar('fig-01-arquitetura.png')


def caminho_do_evento():
    """Um evento, do commit ao efeito: onde cada garantia nasce."""
    t = Tela(2000, 1000)
    t.centrado('O caminho de um evento: publicação transacional, ordem e idempotência', titulo, 1000, 26)

    w, h, y = 430, 270, 140
    xs = [40, 530, 1020, 1510]
    t.caixa(xs[0], y, w, h, '1. Passo da Saga', ['transação local única:', 'UPDATE reservas', 'INSERT outbox_mensagens', '(+ traceparent, correlationId)', 'COMMIT'])
    t.caixa(xs[1], y, w, h, '2. Relay do outbox', ['lê pendentes por ordem de id', 'send com chave reservaId', 'espera ack (acks=all)', 'só então marca publicado', 'erro: para a rodada'])
    t.caixa(xs[2], y, w, h, '3. Kafka', ['partição = hash(reservaId) % 3', 'eventos da reserva em fila', 'produtor idempotente'], fill=BROKER)
    t.caixa(xs[3], y, w, h, '4. Consumidor', ['1 thread por partição', 'eventId na inbox? ignora', 'senão: inbox + efeito', 'no mesmo COMMIT', 'ack do offset'], fill=AUXILIAR)
    for a, b in zip(xs, xs[1:]):
        t.seta((a + w, y + h / 2), (b, y + h / 2))

    t.etiqueta('nada se perde: o evento existe se e só se o estado mudou', 480, 440, cor=AZUL)
    t.etiqueta('pelo menos uma vez: pode repetir', 1230, 440, cor=AZUL)
    t.etiqueta('efeito uma vez só: a inbox absorve a repetição', 1620, 470, cor=VERDE)

    t.caixa(1510, 560, 430, 150, 'Falha no efeito', ['rollback de inbox e efeito', 'nova tentativa: 1 s, depois 2 s'], fill=FALHA, borda=VERMELHO)
    t.seta((1880, 410), (1880, 560), cor=VERMELHO)
    t.caixa(1020, 560, 430, 150, 'DLT do consumidor', ['mesma partição de origem', 'consumo da partição segue'], fill=FALHA, borda=VERMELHO, tracejada=True)
    t.seta((1510, 635), (1450, 635), cor=VERMELHO)
    t.caixa(530, 560, 430, 150, 'Reprocessamento', ['depois de corrigir a causa', 'mesmo caminho idempotente'], fill=FALHA, borda=VERMELHO, tracejada=True)
    t.seta((1020, 635), (960, 635), cor=VERMELHO)

    t.d.rounded_rectangle([40, 790, 1960, 960], radius=16, fill=OBS, outline=TRACO, width=3)
    t.centrado('Em todas as etapas', rotulo, 1000, 805)
    t.centrado('o MDC de cada log leva correlationId, reservaId, eventId e eventType (Kibana);', corpo, 1000, 855)
    t.centrado('o traceparent salvo no outbox liga relay, envio e consumo ao trace da requisição (Zipkin)', corpo, 1000, 895)
    t.salvar('fig-02-caminho-do-evento.png')


if __name__ == '__main__':
    os.makedirs(DESTINO, exist_ok=True)
    arquitetura()
    caminho_do_evento()
