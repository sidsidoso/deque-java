package br.com.sidsidoso.deque;

/**
 * Maximo em janela deslizante (<i>sliding window maximum</i>) em O(n) com uma
 * fila dupla monotonica.
 *
 * <p>A fila guarda <b>indices</b> cujos valores sao estritamente decrescentes
 * do inicio para o fim. Para cada novo elemento:
 * <ol>
 *   <li>remove do inicio o indice que saiu da janela;</li>
 *   <li>remove do fim os indices cujo valor e menor ou igual ao novo, pois eles
 *       nunca mais poderao ser maximo (o novo e maior e sai da janela depois);</li>
 *   <li>insere o novo indice no fim; o inicio da fila e o maximo da janela.</li>
 * </ol>
 * Cada indice entra e sai da fila no maximo uma vez, entao o total e O(n),
 * contra O(n*k) da forca bruta.
 */
public final class JanelaDeslizante {

    private JanelaDeslizante() {
    }

    /**
     * Calcula o maximo de cada janela de tamanho {@code k}.
     *
     * @param valores sequencia de entrada
     * @param k tamanho da janela, {@code 1 <= k <= valores.length}
     * @return vetor com {@code valores.length - k + 1} maximos
     * @throws IllegalArgumentException se {@code k} for invalido
     */
    public static int[] maximos(int[] valores, int k) {
        if (k < 1 || k > valores.length) {
            throw new IllegalArgumentException("janela " + k + " invalida para " + valores.length + " valores");
        }
        int[] resultado = new int[valores.length - k + 1];
        FilaDupla<Integer> indices = new FilaDupla<>();
        for (int i = 0; i < valores.length; i++) {
            if (!indices.vazia() && indices.espiarInicio() <= i - k) {
                indices.removerInicio();
            }
            while (!indices.vazia() && valores[indices.espiarFim()] <= valores[i]) {
                indices.removerFim();
            }
            indices.inserirFim(i);
            if (i >= k - 1) {
                resultado[i - k + 1] = valores[indices.espiarInicio()];
            }
        }
        return resultado;
    }

    /**
     * Versao ingenua O(n*k), usada como referencia nos testes e na demonstracao.
     *
     * @param valores sequencia de entrada
     * @param k tamanho da janela
     * @return vetor com os maximos de cada janela
     */
    public static int[] maximosForcaBruta(int[] valores, int k) {
        if (k < 1 || k > valores.length) {
            throw new IllegalArgumentException("janela " + k + " invalida para " + valores.length + " valores");
        }
        int[] resultado = new int[valores.length - k + 1];
        for (int i = 0; i < resultado.length; i++) {
            int maximo = valores[i];
            for (int j = i + 1; j < i + k; j++) {
                maximo = Math.max(maximo, valores[j]);
            }
            resultado[i] = maximo;
        }
        return resultado;
    }
}
