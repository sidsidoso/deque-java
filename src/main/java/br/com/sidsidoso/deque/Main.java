package br.com.sidsidoso.deque;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/** Demonstracao da fila dupla e de suas aplicacoes. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        demoOperacoesBasicas();
        demoBufferCircular();
        demoPalindromos();
        demoJanelaDeslizante();
    }

    private static void demoOperacoesBasicas() {
        titulo("1. Operacoes basicas");
        FilaDupla<String> fila = new FilaDupla<>();
        fila.inserirFim("B");
        System.out.printf("inserirFim(B)    -> %s%n", fila);
        fila.inserirFim("C");
        System.out.printf("inserirFim(C)    -> %s%n", fila);
        fila.inserirInicio("A");
        System.out.printf("inserirInicio(A) -> %s%n", fila);
        fila.inserirFim("D");
        System.out.printf("inserirFim(D)    -> %s%n", fila);
        System.out.printf("espiarInicio()   -> %s, espiarFim() -> %s%n", fila.espiarInicio(), fila.espiarFim());
        System.out.printf("obter(2)         -> %s%n", fila.obter(2));
        System.out.printf("removerInicio()  -> %s, fila = %s%n", fila.removerInicio(), fila);
        System.out.printf("removerFim()     -> %s, fila = %s%n", fila.removerFim(), fila);

        fila.inserirInicio("A");
        fila.inserirFim("D");
        System.out.print("iterando (inicio -> fim):");
        for (String s : fila) {
            System.out.print(" " + s);
        }
        System.out.println();
        System.out.print("iterando (fim -> inicio):");
        for (String s : fila.reverso()) {
            System.out.print(" " + s);
        }
        System.out.println();

        fila.limpar();
        try {
            fila.removerFim();
        } catch (FilaDuplaVaziaException e) {
            System.out.println("removerFim() em fila vazia -> FilaDuplaVaziaException: " + e.getMessage());
        }
    }

    private static void demoBufferCircular() {
        titulo("2. Buffer circular (capacidade 8, mascara 0b111)");
        FilaDupla<String> fila = new FilaDupla<>();
        System.out.printf("%-26s %s%n", "nova", fila.estadoInterno());
        for (String s : new String[] {"C", "D", "E"}) {
            fila.inserirFim(s);
        }
        System.out.printf("%-26s %s%n", "inserirFim C, D, E", fila.estadoInterno());
        fila.inserirInicio("B");
        fila.inserirInicio("A");
        System.out.printf("%-26s %s%n", "inserirInicio B, A", fila.estadoInterno());
        System.out.printf("%-26s %s%n", "visao logica", fila);
        fila.removerFim();
        fila.removerInicio();
        System.out.printf("%-26s %s%n", "removerFim, removerInicio", fila.estadoInterno());

        System.out.println();
        System.out.println("Capacidade do array interno (cresce 2x; encolhe pela metade a 1/4):");
        FilaDupla<Integer> numeros = new FilaDupla<>();
        StringBuilder trilha = new StringBuilder("  inserindo 1..40 alternando as pontas: ");
        int ultima = numeros.capacidade();
        trilha.append(ultima);
        for (int i = 1; i <= 40; i++) {
            if (i % 2 == 0) {
                numeros.inserirInicio(i);
            } else {
                numeros.inserirFim(i);
            }
            if (numeros.capacidade() != ultima) {
                ultima = numeros.capacidade();
                trilha.append(" -> ").append(ultima).append(" (n=").append(i).append(')');
            }
        }
        System.out.println(trilha);
        trilha = new StringBuilder("  removendo tudo pelo inicio: ").append(ultima);
        while (!numeros.vazia()) {
            numeros.removerInicio();
            if (numeros.capacidade() != ultima) {
                ultima = numeros.capacidade();
                trilha.append(" -> ").append(ultima).append(" (n=").append(numeros.tamanho()).append(')');
            }
        }
        System.out.println(trilha);
    }

    private static void demoPalindromos() {
        titulo("3. Verificador de palindromos");
        String[] textos = {
            "arara",
            "Socorram-me, subi no onibus em Marrocos",
            "A base do teto desaba",
            "Anotaram a data da maratona",
            "estrutura de dados",
            "12321",
            "",
        };
        for (String t : textos) {
            System.out.printf("%-44s %s%n", '"' + t + '"', Palindromo.ehPalindromo(t) ? "palindromo" : "NAO e palindromo");
        }
    }

    private static void demoJanelaDeslizante() {
        titulo("4. Maximo em janela deslizante (fila dupla monotonica)");
        int[] v = {1, 3, -1, -3, 5, 3, 6, 7};
        int k = 3;
        System.out.println("valores : " + Arrays.toString(v));
        System.out.println("k       : " + k);
        System.out.println("maximos : " + Arrays.toString(JanelaDeslizante.maximos(v, k)));
        System.out.println();
        int[] temperaturas = {22, 24, 27, 25, 21, 19, 23, 28, 30, 26, 24, 22, 25, 29};
        System.out.println("temperaturas diarias : " + Arrays.toString(temperaturas));
        System.out.println("maxima semanal (k=7) : " + Arrays.toString(JanelaDeslizante.maximos(temperaturas, 7)));

        System.out.println();
        System.out.println("Comparacao de tempo com a forca bruta O(n*k) (n = 200000):");
        Random rnd = new Random(7);
        int[] grande = new int[200_000];
        for (int i = 0; i < grande.length; i++) {
            grande[i] = rnd.nextInt(1_000_000);
        }
        for (int i = 0; i < 5; i++) { // aquecimento do JIT antes de medir
            JanelaDeslizante.maximos(grande, 50);
            JanelaDeslizante.maximosForcaBruta(grande, 50);
        }
        System.out.printf("  %6s  %14s  %14s  %s%n", "k", "fila dupla", "forca bruta", "iguais");
        for (int janela : new int[] {10, 100, 1_000, 5_000}) {
            long t0 = System.nanoTime();
            int[] rapido = JanelaDeslizante.maximos(grande, janela);
            long t1 = System.nanoTime();
            int[] lento = JanelaDeslizante.maximosForcaBruta(grande, janela);
            long t2 = System.nanoTime();
            System.out.printf(Locale.ROOT, "  %6d  %11.1f ms  %11.1f ms  %s%n", janela,
                    (t1 - t0) / 1e6, (t2 - t1) / 1e6, Arrays.equals(rapido, lento) ? "sim" : "NAO");
        }
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println(texto);
        System.out.println("=".repeat(texto.length()));
    }
}
