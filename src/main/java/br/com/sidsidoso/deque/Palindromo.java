package br.com.sidsidoso.deque;

import java.text.Normalizer;

/**
 * Verificacao de palindromos com fila dupla: os caracteres relevantes entram
 * na fila e sao comparados aos pares, retirando um de cada ponta, ate sobrar
 * no maximo um.
 */
public final class Palindromo {

    private Palindromo() {
    }

    /**
     * Indica se o texto e um palindromo, ignorando maiusculas/minusculas,
     * acentos, espacos e pontuacao. Ex.: "Socorram-me, subi no onibus em
     * Marrocos" e palindromo.
     *
     * @param texto texto a verificar
     * @return {@code true} se for palindromo (texto sem letras/digitos tambem conta)
     */
    public static boolean ehPalindromo(String texto) {
        FilaDupla<Character> fila = new FilaDupla<>();
        for (char c : normalizar(texto).toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                fila.inserirFim(Character.toLowerCase(c));
            }
        }
        while (fila.tamanho() > 1) {
            if (!fila.removerInicio().equals(fila.removerFim())) {
                return false;
            }
        }
        return true;
    }

    /** Decompoe letras acentuadas (NFD) e descarta as marcas diacriticas. */
    static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
