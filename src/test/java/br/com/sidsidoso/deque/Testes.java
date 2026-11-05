package br.com.sidsidoso.deque;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;

/**
 * Testes com um mini framework proprio (sem JUnit). Cada teste e um metodo
 * estatico registrado em {@link #main}; uma falha de assercao lanca
 * {@link AssertionError}. Sai com codigo 1 se algum teste falhar.
 */
public final class Testes {

    private static int passaram;
    private static final List<String> falhas = new ArrayList<>();

    private Testes() {
    }

    public static void main(String[] args) {
        // FilaDupla
        teste("fila dupla nova esta vazia", Testes::novaVazia);
        teste("remover/espiar em fila vazia lancam excecao", Testes::operacoesEmFilaVazia);
        teste("um elemento visto pelas duas pontas", Testes::umElemento);
        teste("ordem FIFO (inserirFim + removerInicio)", Testes::ordemFifo);
        teste("ordem LIFO pelas duas pontas", Testes::ordemLifo);
        teste("rejeita null", Testes::rejeitaNull);
        teste("acesso por indice e definir", Testes::acessoPorIndice);
        teste("indice fora dos limites", Testes::indiceForaDosLimites);
        teste("indices corretos com o buffer dando a volta", Testes::indicesComVolta);
        teste("capacidade e sempre potencia de 2", Testes::capacidadePotenciaDeDois);
        teste("cresce dobrando e preserva a ordem com volta", Testes::crescimentoComVolta);
        teste("encolhe com histerese (1/4 -> metade)", Testes::encolhimento);
        teste("nunca encolhe abaixo da capacidade minima", Testes::capacidadeMinima);
        teste("sem thrashing na fronteira de capacidade", Testes::semThrashing);
        teste("anula referencias ao remover", Testes::anulaReferencias);
        teste("limpar", Testes::limpar);
        teste("iterador direto e reverso", Testes::iteradores);
        teste("iteradores em fila vazia", Testes::iteradoresVazios);
        teste("iteradores sao fail-fast", Testes::iteradoresFailFast);
        teste("definir nao invalida iteradores", Testes::definirNaoInvalida);
        teste("toString do inicio para o fim", Testes::representacaoTexto);
        teste("operacoes aleatorias vs ArrayDeque (oraculo)", Testes::aleatorioContraArrayDeque);
        // Palindromo
        teste("palindromos", Testes::palindromos);
        teste("nao palindromos", Testes::naoPalindromos);
        // Janela deslizante
        teste("janela deslizante: exemplo classico", Testes::janelaExemplo);
        teste("janela deslizante: bordas (k=1, k=n, repetidos)", Testes::janelaBordas);
        teste("janela deslizante: k invalido", Testes::janelaInvalida);
        teste("janela deslizante aleatoria vs forca bruta", Testes::janelaAleatoria);

        System.out.println();
        if (!falhas.isEmpty()) {
            System.out.println(falhas.size() + " teste(s) falharam:");
            falhas.forEach(f -> System.out.println("  - " + f));
            System.out.println(passaram + " testes passaram");
            System.exit(1);
        }
        System.out.println(passaram + " testes passaram");
    }

    // ------------------------------------------------------------ mini framework

    private interface CorpoTeste {
        void executar() throws Exception;
    }

    private static void teste(String nome, CorpoTeste corpo) {
        try {
            corpo.executar();
            passaram++;
            System.out.println("[ OK ] " + nome);
        } catch (Throwable t) {
            falhas.add(nome + ": " + t);
            System.out.println("[FALHOU] " + nome + " -> " + t);
        }
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }

    private static void igual(Object esperado, Object obtido) {
        if (!Objects.equals(esperado, obtido)) {
            throw new AssertionError("esperado <" + esperado + "> mas foi <" + obtido + ">");
        }
    }

    private static <E extends Throwable> E lanca(Class<E> tipo, CorpoTeste corpo) {
        try {
            corpo.executar();
        } catch (Throwable t) {
            if (tipo.isInstance(t)) {
                return tipo.cast(t);
            }
            throw new AssertionError("esperava " + tipo.getSimpleName() + " mas foi " + t, t);
        }
        throw new AssertionError("esperava " + tipo.getSimpleName() + " mas nada foi lancado");
    }

    private static Object[] dadosInternos(FilaDupla<?> f) throws Exception {
        Field campo = FilaDupla.class.getDeclaredField("dados");
        campo.setAccessible(true);
        return (Object[]) campo.get(f);
    }

    private static List<Integer> paraLista(Iterable<Integer> it) {
        List<Integer> l = new ArrayList<>();
        it.forEach(l::add);
        return l;
    }

    // ------------------------------------------------------------ fila dupla

    private static void novaVazia() {
        FilaDupla<Integer> f = new FilaDupla<>();
        verificar(f.vazia(), "deveria estar vazia");
        igual(0, f.tamanho());
        igual(FilaDupla.CAPACIDADE_MINIMA, f.capacidade());
    }

    private static void operacoesEmFilaVazia() {
        FilaDupla<Integer> f = new FilaDupla<>();
        lanca(FilaDuplaVaziaException.class, f::removerInicio);
        lanca(FilaDuplaVaziaException.class, f::removerFim);
        lanca(FilaDuplaVaziaException.class, f::espiarInicio);
        lanca(FilaDuplaVaziaException.class, f::espiarFim);
        f.inserirFim(1);
        f.removerInicio();
        lanca(FilaDuplaVaziaException.class, f::removerFim);
    }

    private static void umElemento() {
        FilaDupla<String> f = new FilaDupla<>();
        f.inserirInicio("x");
        igual("x", f.espiarInicio());
        igual("x", f.espiarFim());
        igual("x", f.obter(0));
        igual("x", f.removerFim());
        verificar(f.vazia(), "deveria ficar vazia");
    }

    private static void ordemFifo() {
        FilaDupla<Integer> f = new FilaDupla<>();
        for (int i = 0; i < 100; i++) {
            f.inserirFim(i);
        }
        for (int i = 0; i < 100; i++) {
            igual(i, f.removerInicio());
        }
    }

    private static void ordemLifo() {
        FilaDupla<Integer> a = new FilaDupla<>();
        FilaDupla<Integer> b = new FilaDupla<>();
        for (int i = 0; i < 100; i++) {
            a.inserirInicio(i);
            b.inserirFim(i);
        }
        for (int i = 99; i >= 0; i--) {
            igual(i, a.removerInicio());
            igual(i, b.removerFim());
        }
    }

    private static void rejeitaNull() {
        FilaDupla<String> f = new FilaDupla<>();
        lanca(NullPointerException.class, () -> f.inserirInicio(null));
        lanca(NullPointerException.class, () -> f.inserirFim(null));
        f.inserirFim("a");
        lanca(NullPointerException.class, () -> f.definir(0, null));
        igual(1, f.tamanho());
    }

    private static void acessoPorIndice() {
        FilaDupla<Integer> f = new FilaDupla<>();
        for (int i = 0; i < 5; i++) {
            f.inserirFim(i);       // 0 1 2 3 4
        }
        f.inserirInicio(-1);       // -1 0 1 2 3 4
        for (int i = 0; i < f.tamanho(); i++) {
            igual(i - 1, f.obter(i));
        }
        igual(2, f.definir(3, 20));
        igual(20, f.obter(3));
        igual("[-1, 0, 1, 20, 3, 4]", f.toString());
    }

    private static void indiceForaDosLimites() {
        FilaDupla<Integer> f = new FilaDupla<>();
        lanca(IndexOutOfBoundsException.class, () -> f.obter(0));
        f.inserirFim(1);
        lanca(IndexOutOfBoundsException.class, () -> f.obter(-1));
        lanca(IndexOutOfBoundsException.class, () -> f.obter(1));
        lanca(IndexOutOfBoundsException.class, () -> f.definir(1, 5));
    }

    private static void indicesComVolta() throws Exception {
        FilaDupla<Integer> f = new FilaDupla<>();
        for (int i = 3; i < 7; i++) {
            f.inserirFim(i);
        }
        for (int i = 2; i >= 0; i--) {
            f.inserirInicio(i);        // inicio da a volta para o fim do array
        }
        igual(8, f.capacidade());
        Object[] dados = dadosInternos(f);
        igual(0, dados[5]);            // (0 - 3) & 7 == 5
        for (int i = 0; i < 7; i++) {
            igual(i, f.obter(i));
        }
    }

    private static void capacidadePotenciaDeDois() {
        Random rnd = new Random(1);
        FilaDupla<Integer> f = new FilaDupla<>();
        for (int i = 0; i < 20_000; i++) {
            if (rnd.nextInt(3) > 0 || f.vazia()) {
                f.inserirInicio(i);
            } else {
                f.removerFim();
            }
            int c = f.capacidade();
            verificar(c >= FilaDupla.CAPACIDADE_MINIMA && Integer.bitCount(c) == 1, "capacidade " + c);
        }
    }

    private static void crescimentoComVolta() {
        FilaDupla<Integer> f = new FilaDupla<>();
        // desloca o inicio para o meio do array antes de crescer
        for (int i = 0; i < 5; i++) {
            f.inserirFim(-1);
        }
        for (int i = 0; i < 5; i++) {
            f.removerInicio();
        }
        for (int i = 0; i < 8; i++) {
            f.inserirFim(i);
        }
        igual(8, f.capacidade());
        f.inserirFim(8);
        igual(16, f.capacidade());
        for (int i = 9; i < 1000; i++) {
            f.inserirFim(i);
        }
        igual(1024, f.capacidade());
        for (int i = 0; i < 1000; i++) {
            igual(i, f.obter(i));
        }
    }

    private static void encolhimento() {
        FilaDupla<Integer> f = new FilaDupla<>();
        for (int i = 0; i < 64; i++) {
            f.inserirFim(i);
        }
        igual(64, f.capacidade());
        while (f.tamanho() > 17) {
            f.removerInicio();
        }
        igual(64, f.capacidade());   // 17 > 64/4: ainda nao encolhe
        f.removerFim();
        igual(32, f.capacidade());   // 16 == 64/4: encolhe para a metade
        while (f.tamanho() > 8) {
            f.removerInicio();
        }
        igual(16, f.capacidade());
        igual(List.of(55, 56, 57, 58, 59, 60, 61, 62), paraLista(f));
    }

    private static void capacidadeMinima() {
        FilaDupla<Integer> f = new FilaDupla<>();
        for (int i = 0; i < 9; i++) {
            f.inserirInicio(i);
        }
        while (!f.vazia()) {
            f.removerFim();
        }
        igual(FilaDupla.CAPACIDADE_MINIMA, f.capacidade());
    }

    private static void semThrashing() {
        FilaDupla<Integer> f = new FilaDupla<>();
        for (int i = 0; i < 17; i++) {
            f.inserirFim(i);
        }
        igual(32, f.capacidade());
        for (int i = 0; i < 50; i++) {
            f.removerInicio();
            igual(32, f.capacidade());
            f.inserirInicio(i);
            igual(32, f.capacidade());
        }
    }

    private static void anulaReferencias() throws Exception {
        FilaDupla<String> f = new FilaDupla<>();
        f.inserirFim("a");
        f.inserirFim("b");
        f.inserirFim("c");
        f.removerInicio();
        f.removerFim();
        long ocupadas = Arrays.stream(dadosInternos(f)).filter(Objects::nonNull).count();
        igual(1L, ocupadas);
    }

    private static void limpar() {
        FilaDupla<Integer> f = new FilaDupla<>();
        for (int i = 0; i < 100; i++) {
            f.inserirFim(i);
        }
        f.limpar();
        verificar(f.vazia(), "deveria estar vazia");
        igual(FilaDupla.CAPACIDADE_MINIMA, f.capacidade());
        f.inserirInicio(7);
        igual(7, f.espiarFim());
    }

    private static void iteradores() {
        FilaDupla<Integer> f = new FilaDupla<>();
        for (int i = 5; i < 10; i++) {
            f.inserirFim(i);
        }
        for (int i = 4; i >= 0; i--) {
            f.inserirInicio(i);
        }
        igual(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), paraLista(f));
        igual(List.of(9, 8, 7, 6, 5, 4, 3, 2, 1, 0), paraLista(f.reverso()));
        igual(10, f.tamanho()); // iterar nao remove
    }

    private static void iteradoresVazios() {
        FilaDupla<String> f = new FilaDupla<>();
        for (Iterator<String> it : List.of(f.iterator(), f.iteradorReverso())) {
            verificar(!it.hasNext(), "iterador de fila vazia nao tem proximo");
            lanca(NoSuchElementException.class, it::next);
        }
    }

    private static void iteradoresFailFast() {
        FilaDupla<Integer> f = new FilaDupla<>();
        f.inserirFim(1);
        f.inserirFim(2);
        Iterator<Integer> direto = f.iterator();
        Iterator<Integer> reverso = f.iteradorReverso();
        direto.next();
        reverso.next();
        f.inserirInicio(0);
        lanca(ConcurrentModificationException.class, direto::next);
        lanca(ConcurrentModificationException.class, reverso::next);
        Iterator<Integer> outro = f.iterator();
        f.removerFim();
        lanca(ConcurrentModificationException.class, outro::next);
    }

    private static void definirNaoInvalida() {
        FilaDupla<Integer> f = new FilaDupla<>();
        f.inserirFim(1);
        f.inserirFim(2);
        Iterator<Integer> it = f.iterator();
        igual(1, it.next());
        f.definir(1, 99);
        igual(99, it.next());
    }

    private static void representacaoTexto() {
        FilaDupla<Integer> f = new FilaDupla<>();
        igual("[]", f.toString());
        f.inserirFim(2);
        f.inserirFim(3);
        f.inserirInicio(1);
        igual("[1, 2, 3]", f.toString());
    }

    private static void aleatorioContraArrayDeque() {
        Random rnd = new Random(42);
        FilaDupla<Integer> fila = new FilaDupla<>();
        ArrayDeque<Integer> oraculo = new ArrayDeque<>();
        for (int passo = 0; passo < 300_000; passo++) {
            // fases que favorecem crescer ou encolher, para exercitar o redimensionamento
            boolean faseCrescer = (passo / 7_000) % 2 == 0;
            int r = rnd.nextInt(100);
            if (r < (faseCrescer ? 30 : 17)) {
                int x = rnd.nextInt();
                fila.inserirInicio(x);
                oraculo.addFirst(x);
            } else if (r < (faseCrescer ? 60 : 35)) {
                int x = rnd.nextInt();
                fila.inserirFim(x);
                oraculo.addLast(x);
            } else if (r < 70) {
                if (oraculo.isEmpty()) {
                    lanca(FilaDuplaVaziaException.class, fila::removerInicio);
                } else {
                    igual(oraculo.pollFirst(), fila.removerInicio());
                }
            } else if (r < 90) {
                if (oraculo.isEmpty()) {
                    lanca(FilaDuplaVaziaException.class, fila::removerFim);
                } else {
                    igual(oraculo.pollLast(), fila.removerFim());
                }
            } else if (r < 95) {
                if (oraculo.isEmpty()) {
                    lanca(FilaDuplaVaziaException.class, fila::espiarInicio);
                } else {
                    igual(oraculo.peekFirst(), fila.espiarInicio());
                    igual(oraculo.peekFirst(), fila.obter(0));
                }
            } else if (oraculo.isEmpty()) {
                lanca(FilaDuplaVaziaException.class, fila::espiarFim);
            } else {
                igual(oraculo.peekLast(), fila.espiarFim());
                igual(oraculo.peekLast(), fila.obter(fila.tamanho() - 1));
            }
            igual(oraculo.size(), fila.tamanho());
            igual(oraculo.isEmpty(), fila.vazia());
            verificar(fila.capacidade() >= fila.tamanho(), "capacidade menor que o tamanho");
            if (passo % 10_000 == 0) {
                compararConteudo(oraculo, fila);
            }
        }
        compararConteudo(oraculo, fila);
    }

    /** Compara iteracao direta, reversa e acesso por indice com o oraculo. */
    private static void compararConteudo(ArrayDeque<Integer> oraculo, FilaDupla<Integer> fila) {
        Object[] esperado = oraculo.toArray();
        igual(Arrays.asList(esperado), new ArrayList<Object>(paraLista(fila)));
        for (int i = 0; i < esperado.length; i++) {
            igual(esperado[i], fila.obter(i));
        }
        Iterator<Integer> reverso = oraculo.descendingIterator();
        for (int x : fila.reverso()) {
            igual(reverso.next(), x);
        }
        verificar(!reverso.hasNext(), "iteracao reversa com tamanhos diferentes");
    }

    // ------------------------------------------------------------ palindromo

    private static void palindromos() {
        for (String s : List.of("", "a", "aa", "arara", "Ovo", "12321",
                "Socorram-me, subi no onibus em Marrocos",
                "Anotaram a data da maratona",
                "A torre da derrota",
                "Após a sopa", "!!! ...")) {
            verificar(Palindromo.ehPalindromo(s), "deveria ser palindromo: " + s);
        }
    }

    private static void naoPalindromos() {
        for (String s : List.of("ab", "abca", "estrutura", "123421", "Roma nao e amor")) {
            verificar(!Palindromo.ehPalindromo(s), "nao deveria ser palindromo: " + s);
        }
    }

    // ------------------------------------------------------------ janela deslizante

    private static void janelaExemplo() {
        int[] v = {1, 3, -1, -3, 5, 3, 6, 7};
        igual("[3, 3, 5, 5, 6, 7]", Arrays.toString(JanelaDeslizante.maximos(v, 3)));
    }

    private static void janelaBordas() {
        int[] v = {4, 2, 12, 3, 8};
        igual(Arrays.toString(v), Arrays.toString(JanelaDeslizante.maximos(v, 1)));
        igual("[12]", Arrays.toString(JanelaDeslizante.maximos(v, 5)));
        igual("[5, 5, 5, 5]", Arrays.toString(JanelaDeslizante.maximos(new int[] {5, 5, 5, 5, 5}, 2)));
        igual("[9, 8, 7]", Arrays.toString(JanelaDeslizante.maximos(new int[] {9, 8, 7, 6, 5}, 3)));
        igual("[7, 8, 9]", Arrays.toString(JanelaDeslizante.maximos(new int[] {5, 6, 7, 8, 9}, 3)));
    }

    private static void janelaInvalida() {
        lanca(IllegalArgumentException.class, () -> JanelaDeslizante.maximos(new int[] {1, 2}, 0));
        lanca(IllegalArgumentException.class, () -> JanelaDeslizante.maximos(new int[] {1, 2}, 3));
        lanca(IllegalArgumentException.class, () -> JanelaDeslizante.maximos(new int[0], 1));
    }

    private static void janelaAleatoria() {
        Random rnd = new Random(2026);
        for (int rodada = 0; rodada < 500; rodada++) {
            int n = 1 + rnd.nextInt(300);
            int[] v = new int[n];
            int faixa = 1 + rnd.nextInt(50); // faixas pequenas geram muitos repetidos
            for (int i = 0; i < n; i++) {
                v[i] = rnd.nextInt(faixa) - faixa / 2;
            }
            int k = 1 + rnd.nextInt(n);
            verificar(Arrays.equals(JanelaDeslizante.maximosForcaBruta(v, k), JanelaDeslizante.maximos(v, k)),
                    "divergencia para n=" + n + ", k=" + k);
        }
    }
}
