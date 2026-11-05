# Fila Dupla (Deque) em Java

[![CI](https://github.com/sidsidoso/deque-java/actions/workflows/ci.yml/badge.svg)](https://github.com/sidsidoso/deque-java/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
[![Licença: MIT](https://img.shields.io/badge/licen%C3%A7a-MIT-blue.svg)](LICENSE)

Implementação de uma **fila dupla genérica** (*double-ended queue*, ou *deque*)
sobre **array circular**, feita do zero em Java 21 para a disciplina de
Estrutura de Dados, junto com duas aplicações clássicas:

- **verificador de palíndromos**, que compara os caracteres retirando um de
  cada ponta;
- **máximo em janela deslizante** (*sliding window maximum*) em O(n), com uma
  fila dupla monotônica de índices.

Sem dependências externas: compila com `javac` puro e os testes usam um mini
framework próprio.

## Conceito

Uma fila dupla permite inserir, remover e consultar nas **duas pontas**. Ela
generaliza tanto a pilha (usando uma ponta só) quanto a fila (entrando por uma
ponta e saindo pela outra).

```
 inserirInicio(x)                                   inserirFim(x)
 removerInicio()  ──►  ┌───┬───┬───┬───┬───┐  ◄──  removerFim()
 espiarInicio()        │ A │ B │ C │ D │ E │        espiarFim()
                       └───┴───┴───┴───┴───┘
                         0   1   2   3   4    <- obter(i) em O(1)
```

Internamente, os elementos ficam em um **array circular**: o campo `inicio`
aponta para o primeiro elemento e o fim é `inicio + tamanho - 1`, dando a volta
no array quando passa da última posição. Inserir no início apenas **recua**
`inicio` uma posição; nada precisa ser deslocado.

```
 posição:    0     1     2     3     4     5     6     7
          ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
 dados:   │  C  │  D  │  E  │  .  │  .  │  .  │  A  │  B  │   capacidade = 8
          └─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┘
                        ▲                       ▲
                        fim                     inicio = 6

 visão lógica: [A, B, C, D, E]     obter(i) = dados[(inicio + i) & 7]
```

Como a capacidade é sempre uma **potência de 2**, a volta é feita com uma
máscara de bits em vez do resto da divisão:

```
 (6 + 3) % 8  == 1        (6 + 3) & 0b111 == 1        mesma coisa, sem divisão
 (0 - 1) % 8  == -1  (!)  (0 - 1) & 0b111 == 7        e funciona para negativos
```

## Operações e complexidade

| Operação                         | Descrição                                     | Tempo           |
|----------------------------------|-----------------------------------------------|-----------------|
| `inserirInicio(x)` / `inserirFim(x)` | insere em uma das pontas                  | O(1) amortizado |
| `removerInicio()` / `removerFim()`   | remove e devolve a ponta                  | O(1) amortizado |
| `espiarInicio()` / `espiarFim()`     | consulta a ponta sem remover              | O(1)            |
| `obter(i)` / `definir(i, x)`     | acesso por índice lógico                      | O(1)            |
| `tamanho()` / `vazia()`          | quantidade de elementos                       | O(1)            |
| `limpar()`                       | remove tudo e volta à capacidade mínima       | O(1)            |
| `iterator()` / `reverso()`       | percorre início → fim ou fim → início         | O(n) no total   |
| `Palindromo.ehPalindromo(s)`     | compara as pontas até o meio                  | O(n)            |
| `JanelaDeslizante.maximos(v, k)` | máximo de cada janela de tamanho `k`          | O(n)            |
| `JanelaDeslizante.maximosForcaBruta(v, k)` | referência ingênua                  | O(n·k)          |

Memória: O(n). Graças à histerese, a capacidade fica sempre entre `n` e `4n`
(respeitando a capacidade mínima de 8).

Remover ou espiar em fila vazia lança `FilaDuplaVaziaException`; índice fora de
`[0, tamanho)` lança `IndexOutOfBoundsException`.

## Decisões de implementação e otimizações

- **Capacidade potência de 2 + máscara**: `(i) & (capacidade - 1)` substitui
  `i % capacidade`. É mais barato que a divisão e, de quebra, resolve o índice
  negativo de `inicio - 1` ao inserir no início (em Java, `-1 % 8 == -1`).
  É a mesma técnica usada por `java.util.ArrayDeque`.
- **Crescimento "desenrolando" o círculo**: ao dobrar (ou encolher), os
  elementos são copiados com no máximo **duas** chamadas a `System.arraycopy`
  (de `inicio` até o fim do array e, se houver volta, do começo do array até o
  último elemento), e o primeiro elemento passa a ocupar a posição 0.
- **Encolhimento com histerese**: o array só cai para a metade quando a
  ocupação chega a **1/4** (e nunca abaixo de 8). Encolher já em 1/2 faria
  operações alternadas na fronteira copiarem o array a cada passo
  (*thrashing*); com 1/4, após qualquer redimensionamento a fila fica pela
  metade, longe dos dois limites.
- **Referências anuladas ao remover**: a posição liberada recebe `null`, para
  não manter vivos objetos que já saíram (*loitering*).
- **Iteradores fail-fast nos dois sentidos**: um contador de modificações
  estruturais faz `next()` lançar `ConcurrentModificationException` se a fila
  for alterada durante a iteração. `definir(i, x)` não muda a estrutura, então
  não invalida iteradores (como `ArrayList.set`).
- **Sem `null` como elemento**: evita ambiguidade com posições livres.
- **Palíndromos**: o texto é normalizado em NFD e as marcas diacríticas são
  descartadas, então acentos, maiúsculas, espaços e pontuação são ignorados
  ("Socorram-me, subi no ônibus em Marrocos" é palíndromo).
- **Janela deslizante monotônica**: a fila guarda **índices** com valores
  estritamente decrescentes. A cada novo valor, sai do início o índice que
  deixou a janela e saem do fim os índices com valor menor ou igual, que nunca
  mais serão máximo. Cada índice entra e sai uma única vez, logo O(n) no total,
  independentemente de `k`.

```
 v = [1, 3, -1, -3, 5, 3, 6, 7], k = 3      (fila mostra os valores dos índices)

 i  v[i]  fila dupla        máximo
 0    1   [1]
 1    3   [3]               (1 sai pelo fim: 1 <= 3)
 2   -1   [3, -1]           3
 3   -3   [3, -1, -3]       3
 4    5   [5]               5   (3 saiu da janela; -1 e -3 saem pelo fim)
 5    3   [5, 3]            5
 6    6   [6]               6
 7    7   [7]               7
```

## Estrutura de pastas

```
deque-java/
├── .github/workflows/ci.yml        # CI: testes + demo no GitHub Actions
├── src/
│   ├── main/java/br/com/sidsidoso/deque/
│   │   ├── FilaDupla.java          # a estrutura de dados
│   │   ├── FilaDuplaVaziaException.java
│   │   ├── Palindromo.java
│   │   ├── JanelaDeslizante.java
│   │   └── Main.java               # demonstração
│   └── test/java/br/com/sidsidoso/deque/
│       └── Testes.java             # mini framework de testes + casos
├── build.sh                        # build para Linux/macOS
├── build.ps1                       # build para Windows
├── LICENSE
└── README.md
```

## Como compilar, executar e testar

Pré-requisito: **JDK 21** ou superior (`javac` e `java` no `PATH`, ou
`JAVA_HOME` definido para o `build.ps1`).

### Com os scripts

```bash
# Linux / macOS
./build.sh          # só compila para out/
./build.sh run      # compila e executa a demonstração
./build.sh test     # compila e roda os testes
```

```powershell
# Windows (PowerShell)
.\build.ps1
.\build.ps1 run
.\build.ps1 test
```

Os scripts compilam com `-Xlint:all -Werror`, então qualquer aviso do
compilador quebra o build.

### Manualmente

```bash
javac --release 21 -Xlint:all -d out $(find src -name "*.java")
java -cp out br.com.sidsidoso.deque.Main      # demonstração
java -cp out br.com.sidsidoso.deque.Testes    # testes
```

O programa de testes imprime cada caso, termina com `N testes passaram` e sai
com código 1 se algum falhar. Entre os casos estão bordas (fila vazia, um
elemento, índices com o buffer dando a volta, crescimento e encolhimento,
ausência de *thrashing*, iteradores fail-fast), **300 mil operações
aleatórias** nas duas pontas comparadas com `java.util.ArrayDeque` como oráculo
(incluindo iteração direta, reversa e acesso por índice) e 500 entradas
aleatórias da janela deslizante comparadas com a força bruta.

## Exemplo de saída

Saída real de `./build.sh run` (os tempos da última tabela variam de máquina
para máquina):

```
1. Operacoes basicas
====================
inserirFim(B)    -> [B]
inserirFim(C)    -> [B, C]
inserirInicio(A) -> [A, B, C]
inserirFim(D)    -> [A, B, C, D]
espiarInicio()   -> A, espiarFim() -> D
obter(2)         -> C
removerInicio()  -> A, fila = [B, C, D]
removerFim()     -> D, fila = [B, C]
iterando (inicio -> fim): A B C D
iterando (fim -> inicio): D C B A
removerFim() em fila vazia -> FilaDuplaVaziaException: impossivel remover do fim de uma fila dupla vazia

2. Buffer circular (capacidade 8, mascara 0b111)
================================================
nova                       [. . . . . . . .] inicio=0
inserirFim C, D, E         [C D E . . . . .] inicio=0
inserirInicio B, A         [C D E . . . A B] inicio=6
visao logica               [A, B, C, D, E]
removerFim, removerInicio  [C D . . . . . B] inicio=7

Capacidade do array interno (cresce 2x; encolhe pela metade a 1/4):
  inserindo 1..40 alternando as pontas: 8 -> 16 (n=9) -> 32 (n=17) -> 64 (n=33)
  removendo tudo pelo inicio: 64 -> 32 (n=16) -> 16 (n=8) -> 8 (n=4)

3. Verificador de palindromos
=============================
"arara"                                      palindromo
"Socorram-me, subi no onibus em Marrocos"    palindromo
"A base do teto desaba"                      palindromo
"Anotaram a data da maratona"                palindromo
"estrutura de dados"                         NAO e palindromo
"12321"                                      palindromo
""                                           palindromo

4. Maximo em janela deslizante (fila dupla monotonica)
======================================================
valores : [1, 3, -1, -3, 5, 3, 6, 7]
k       : 3
maximos : [3, 3, 5, 5, 6, 7]

temperaturas diarias : [22, 24, 27, 25, 21, 19, 23, 28, 30, 26, 24, 22, 25, 29]
maxima semanal (k=7) : [27, 28, 30, 30, 30, 30, 30, 30]

Comparacao de tempo com a forca bruta O(n*k) (n = 200000):
       k      fila dupla     forca bruta  iguais
      10          4.5 ms          1.1 ms  sim
     100          6.1 ms          9.7 ms  sim
    1000          4.9 ms        120.5 ms  sim
    5000          4.9 ms        569.2 ms  sim
```

Repare que o tempo da fila dupla não depende de `k`, enquanto a força bruta
cresce linearmente com a janela. Para `k` muito pequeno a força bruta ainda
vence, porque percorre um `int[]` sem nenhum *boxing* de `Integer`.

## Referências

- SEDGEWICK, R.; WAYNE, K. *Algorithms*. 4. ed. Addison-Wesley, 2011. Seção 1.3
  (filas, deques e arrays redimensionáveis).
- CORMEN, T. H. et al. *Algoritmos: teoria e prática*. 3. ed. Elsevier, 2012.
  Capítulo 10 (pilhas e filas) e Capítulo 17 (análise amortizada).
- KNUTH, D. E. *The Art of Computer Programming*, v. 1. 3. ed. Addison-Wesley,
  1997. Seção 2.2.1 (pilhas, filas e deques).
- [Sliding Window Maximum — LeetCode 239](https://leetcode.com/problems/sliding-window-maximum/)
- [Documentação de `java.util.ArrayDeque`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayDeque.html)
