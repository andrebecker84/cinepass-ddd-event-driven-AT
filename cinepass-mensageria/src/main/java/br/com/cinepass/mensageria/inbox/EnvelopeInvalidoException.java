package br.com.cinepass.mensageria.inbox;

/**
 * Mensagem que não pode ser lida como envelope. Não adianta tentar de novo: o tratamento
 * de falhas a manda direto para a DLT, sem as retentativas.
 */
public class EnvelopeInvalidoException extends RuntimeException {

    public EnvelopeInvalidoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
