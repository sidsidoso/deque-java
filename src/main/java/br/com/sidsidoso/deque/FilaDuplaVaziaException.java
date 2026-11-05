package br.com.sidsidoso.deque;

/** Lancada ao tentar remover ou espiar um elemento de uma fila dupla vazia. */
public class FilaDuplaVaziaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param mensagem descricao do erro */
    public FilaDuplaVaziaException(String mensagem) {
        super(mensagem);
    }
}
