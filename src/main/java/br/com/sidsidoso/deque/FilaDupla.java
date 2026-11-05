package br.com.sidsidoso.deque;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Fila dupla (deque) generica implementada sobre um array circular.
 *
 * <p>A capacidade do array e sempre uma potencia de dois, de modo que a volta
 * ao inicio do array e feita com uma mascara de bits ({@code indice & (cap - 1)})
 * em vez do operador {@code %}. O array dobra quando enche e cai pela metade
 * quando a ocupacao chega a 1/4 (histerese), entao inserir e remover nas duas
 * pontas custa O(1) amortizado e o acesso por indice custa O(1).
 *
 * <p>Nao aceita {@code null} como elemento. Nao e thread-safe.
 *
 * @param <T> tipo dos elementos
 */
public final class FilaDupla<T> implements Iterable<T> {

    /** Capacidade inicial e capacidade minima do array interno (potencia de 2). */
    static final int CAPACIDADE_MINIMA = 8;

    /** Maior potencia de dois representavel como tamanho de array. */
    private static final int CAPACIDADE_MAXIMA = 1 << 30;

    private Object[] dados;
    /** Posicao fisica do primeiro elemento. */
    private int inicio;
    private int tamanho;
    /** Contador de modificacoes estruturais, usado pelos iteradores fail-fast. */
    private int modificacoes;

    /** Cria uma fila dupla vazia com a capacidade minima. */
    public FilaDupla() {
        dados = new Object[CAPACIDADE_MINIMA];
    }

    // ------------------------------------------------------------ insercao

    /**
     * Insere um elemento no inicio (frente) da fila dupla.
     *
     * @param elemento elemento a inserir (nao nulo)
     * @throws NullPointerException se o elemento for nulo
     */
    public void inserirInicio(T elemento) {
        exigirNaoNulo(elemento);
        garantirEspaco();
        inicio = (inicio - 1) & mascara();
        dados[inicio] = elemento;
        tamanho++;
        modificacoes++;
    }

    /**
     * Insere um elemento no fim (tras) da fila dupla.
     *
     * @param elemento elemento a inserir (nao nulo)
     * @throws NullPointerException se o elemento for nulo
     */
    public void inserirFim(T elemento) {
        exigirNaoNulo(elemento);
        garantirEspaco();
        dados[(inicio + tamanho) & mascara()] = elemento;
        tamanho++;
        modificacoes++;
    }

    // ------------------------------------------------------------ remocao

    /**
     * Remove e devolve o primeiro elemento.
     *
     * @return elemento que estava no inicio
     * @throws FilaDuplaVaziaException se a fila dupla estiver vazia
     */
    public T removerInicio() {
        exigirNaoVazia("remover do inicio de");
        T elemento = elementoFisico(inicio);
        dados[inicio] = null; // libera a referencia para o coletor de lixo
        inicio = (inicio + 1) & mascara();
        tamanho--;
        modificacoes++;
        talvezEncolher();
        return elemento;
    }

    /**
     * Remove e devolve o ultimo elemento.
     *
     * @return elemento que estava no fim
     * @throws FilaDuplaVaziaException se a fila dupla estiver vazia
     */
    public T removerFim() {
        exigirNaoVazia("remover do fim de");
        int posicao = (inicio + tamanho - 1) & mascara();
        T elemento = elementoFisico(posicao);
        dados[posicao] = null;
        tamanho--;
        modificacoes++;
        talvezEncolher();
        return elemento;
    }

    /** Remove todos os elementos e volta para a capacidade minima. */
    public void limpar() {
        dados = new Object[CAPACIDADE_MINIMA];
        inicio = 0;
        tamanho = 0;
        modificacoes++;
    }

    // ------------------------------------------------------------ consulta

    /**
     * Devolve o primeiro elemento sem remove-lo.
     *
     * @return elemento do inicio
     * @throws FilaDuplaVaziaException se a fila dupla estiver vazia
     */
    public T espiarInicio() {
        exigirNaoVazia("espiar o inicio de");
        return elementoFisico(inicio);
    }

    /**
     * Devolve o ultimo elemento sem remove-lo.
     *
     * @return elemento do fim
     * @throws FilaDuplaVaziaException se a fila dupla estiver vazia
     */
    public T espiarFim() {
        exigirNaoVazia("espiar o fim de");
        return elementoFisico((inicio + tamanho - 1) & mascara());
    }

    /**
     * Acesso por indice logico em O(1): {@code 0} e o inicio e
     * {@code tamanho() - 1} e o fim.
     *
     * @param indice posicao logica
     * @return elemento na posicao
     * @throws IndexOutOfBoundsException se o indice estiver fora de {@code [0, tamanho)}
     */
    public T obter(int indice) {
        return elementoFisico(posicaoFisica(indice));
    }

    /**
     * Substitui o elemento em um indice logico. Nao e uma modificacao
     * estrutural, portanto nao invalida iteradores.
     *
     * @param indice posicao logica
     * @param elemento novo elemento (nao nulo)
     * @return elemento anterior
     */
    public T definir(int indice, T elemento) {
        exigirNaoNulo(elemento);
        int posicao = posicaoFisica(indice);
        T anterior = elementoFisico(posicao);
        dados[posicao] = elemento;
        return anterior;
    }

    /** @return quantidade de elementos */
    public int tamanho() {
        return tamanho;
    }

    /** @return {@code true} se nao houver elementos */
    public boolean vazia() {
        return tamanho == 0;
    }

    /** @return capacidade atual do array interno (visivel para testes) */
    int capacidade() {
        return dados.length;
    }

    // ------------------------------------------------------------ iteracao

    /**
     * Itera do inicio para o fim. O iterador e fail-fast: lanca
     * {@link ConcurrentModificationException} se a fila dupla sofrer uma
     * modificacao estrutural durante a iteracao.
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterador(false);
    }

    /** @return iterador fail-fast do fim para o inicio */
    public Iterator<T> iteradorReverso() {
        return new Iterador(true);
    }

    /** @return {@link Iterable} que percorre do fim para o inicio (util em for-each) */
    public Iterable<T> reverso() {
        return this::iteradorReverso;
    }

    /** @return representacao do inicio para o fim, ex.: {@code [1, 2, 3]} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < tamanho; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(dados[(inicio + i) & mascara()]);
        }
        return sb.append(']').toString();
    }

    /**
     * Mostra o array fisico, com {@code .} nas posicoes livres, e a posicao do
     * inicio. Ex.: {@code [C D . . . . A B] inicio=6}. Usado na demonstracao.
     *
     * @return estado interno do buffer circular
     */
    String estadoInterno() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < dados.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(dados[i] == null ? "." : dados[i]);
        }
        return sb.append("] inicio=").append(inicio).toString();
    }

    private final class Iterador implements Iterator<T> {
        private final boolean reverso;
        private final int modificacoesEsperadas = modificacoes;
        private int visitados;

        Iterador(boolean reverso) {
            this.reverso = reverso;
        }

        @Override
        public boolean hasNext() {
            return visitados < tamanho;
        }

        @Override
        public T next() {
            if (modificacoes != modificacoesEsperadas) {
                throw new ConcurrentModificationException();
            }
            if (visitados >= tamanho) {
                throw new NoSuchElementException();
            }
            int logico = reverso ? tamanho - 1 - visitados : visitados;
            visitados++;
            return elementoFisico((inicio + logico) & mascara());
        }
    }

    // ------------------------------------------------------------ auxiliares

    private int mascara() {
        return dados.length - 1;
    }

    private int posicaoFisica(int indice) {
        if (indice < 0 || indice >= tamanho) {
            throw new IndexOutOfBoundsException("indice " + indice + " fora de [0, " + tamanho + ")");
        }
        return (inicio + indice) & mascara();
    }

    private void garantirEspaco() {
        if (tamanho == dados.length) {
            if (dados.length == CAPACIDADE_MAXIMA) {
                throw new IllegalStateException("capacidade maxima atingida");
            }
            redimensionar(dados.length * 2);
        }
    }

    private void talvezEncolher() {
        if (tamanho > 0 && tamanho == dados.length / 4 && dados.length > CAPACIDADE_MINIMA) {
            redimensionar(dados.length / 2);
        }
    }

    /**
     * Copia os elementos para um novo array, "desenrolando" o circulo: o
     * primeiro elemento passa a ocupar a posicao 0. Sao no maximo duas copias
     * em bloco: do inicio ate o fim do array e, se houver volta, do comeco do
     * array ate o ultimo elemento.
     */
    private void redimensionar(int novaCapacidade) {
        Object[] novo = new Object[novaCapacidade];
        int ateFimDoArray = Math.min(tamanho, dados.length - inicio);
        System.arraycopy(dados, inicio, novo, 0, ateFimDoArray);
        System.arraycopy(dados, 0, novo, ateFimDoArray, tamanho - ateFimDoArray);
        dados = novo;
        inicio = 0;
    }

    private static void exigirNaoNulo(Object elemento) {
        if (elemento == null) {
            throw new NullPointerException("a fila dupla nao aceita elementos nulos");
        }
    }

    private void exigirNaoVazia(String operacao) {
        if (tamanho == 0) {
            throw new FilaDuplaVaziaException("impossivel " + operacao + " uma fila dupla vazia");
        }
    }

    @SuppressWarnings("unchecked")
    private T elementoFisico(int posicao) {
        return (T) dados[posicao];
    }
}
