# androidVNC2026

Cliente VNC para reaproveitar tablets e celulares Android antigos como terminais de acesso a um computador. Este fork mantém a base do [android-vnc-viewer, da antlersoft](https://github.com/antlersoft/android-vnc-viewer), com melhorias de orientação, escala da imagem e estabilidade.

O foco de validação é o **NT-1710 com Android 4.2.2, 512 MB de RAM e tela de 800 × 480 pixels**. O computador acessado precisa executar um servidor VNC.

## Download e instalação

Baixe `androidVNC2026-vX.Y.Z.apk` na [última release](https://github.com/PatrikGuerra/android-vnc-viewer/releases/latest). Confira sua integridade usando o arquivo `SHA256SUMS.txt` que acompanha o APK.

1. No Android antigo, habilite a instalação de aplicativos de fontes desconhecidas.
2. Abra o APK e instale. Via USB, use `adb install -r androidVNC2026-vX.Y.Z.apk`.
3. Cadastre endereço, porta e senha do servidor, quando necessária, e conecte pela rede local ou privada.

As releases deste fork preservam pacote e certificado, permitindo atualizações sem apagar os perfis. Instalações do projeto original ou de outros distribuidores podem ter certificado diferente: exporte os perfis antes de trocar de distribuição. Não desinstale uma versão com dados importantes sem backup.

## Funcionalidades adicionadas

| Recurso | Comportamento |
| --- | --- |
| **Girar 90°** | Alterna entre horizontal, vertical e suas posições invertidas, inclusive durante uma sessão. |
| **Orientação da tela** | Oferece automática pelo sensor, horizontal, horizontal invertida, vertical e vertical invertida. |
| Preferência persistente | Mantém a orientação ao abrir novamente o aplicativo e ao entrar na sessão. |
| Rotação com sessão ativa | Ajusta a visualização sem recriar a atividade ou reconectar ao servidor. |
| **Preencher tela toda (sem bordas)** | Estica a imagem para ocupar a área disponível; pode alterar suas proporções. |
| Ajustes de toque e desenho | Converte as coordenadas conforme a escala e recalcula o desenho após mudar o tamanho da tela. |

**Girar 90°** e **Orientação da tela** aparecem no menu da configuração e da sessão. Abra-o pelo botão Menu do aparelho ou pelo menu da interface. Na sessão, a escala fica em **Scaling**.

Continuam disponíveis os modos originais **Fit to Screen** (mantém proporções), **1:1** e **Zoomable**. Perfis de conexão, escolha de cores, modos de entrada, teclas especiais e importação/exportação também vêm do projeto original; não foram introduzidos neste fork.

## Correções de estabilidade

- Abertura do menu de configuração no Android 4.2.2.
- Proteção contra redesenho e exibição de informações antes de o framebuffer e o formato de cor estarem prontos.
- Tratamento de conexão recusada sem acessar dados ainda não inicializados.
- Desenho correto no modo de preenchimento, incluindo transformação dos limites dos drawables.
- Mapeamento das quatro rotações considerando a orientação natural do painel.
- Preservação das classes do banco em builds limpos e recuperação da compilação legada no GitHub Actions.

## Especificações do APK

| Item | Especificação |
| --- | --- |
| Nome exibido | androidVNC2026 |
| Pacote | `android.androidVNC` |
| Plataforma validada em aparelho | Android 4.2.2 / API 17, NT-1710 |
| Android mínimo declarado | Android 1.5 / API 3; versões anteriores à 4.2.2 não foram validadas neste fork |
| Target SDK | API 17 |
| SDK de compilação | API 20; a implementação de interface da API 19 é protegida por verificação de versão |
| Ferramentas | Java 8, Ant, SDK Tools 25.2.5 e Build Tools 28.0.3 |
| Distribuição | APK release alinhado, assinado e não depurável |
| Assinatura | JAR/v1, compatível com Android antigo; certificado persistente do fork |
| Permissões declaradas | Internet, gravação no armazenamento externo e manutenção do aparelho acordado (`WAKE_LOCK`) |

SHA-256 do certificado das releases:

```text
185900e3436a64e868ba0b89180f046b90aa239711ca200772488a6f90932a92
```

É o mesmo certificado dos builds de desenvolvimento instalados durante a validação do fork. A chave privada permanece em GitHub Secrets e não faz parte do repositório.

## Releases e versionamento automático

Cada push em `master` testa, compila e verifica o APK antes de publicar. A primeira release é **v1.0.0**, seguida de **v1.0.1**, **v1.0.2** etc. Tags históricas anteriores à série 1 são preservadas e ignoradas nesse cálculo.

- `versionName` corresponde à versão publicada, sem o prefixo `v`.
- `versionCode` é `1.000.000 + github.run_number`, mantendo a ordem das atualizações; falhas podem deixar intervalos.
- A versão é aplicada no workspace do CI, sem commits automáticos no código-fonte.
- Uma reexecução do mesmo commit reutiliza sua tag; releases já publicadas não são sobrescritas.
- PRs, outras branches e execuções manuais apenas validam e geram artefatos de desenvolvimento.
- A publicação exige `ANDROID_DEBUG_KEYSTORE_B64`, segredo que contém a chave persistente existente. O nome histórico não determina o modo do APK: a publicação usa `ant release`.
- Cada release inclui APK, `SHA256SUMS.txt` e notas com o commit de origem. Apenas o job de publicação recebe `contents: write`.

Publicações ficam em fila e não cancelam uma execução em andamento. Se houver falha após criar a release, ela permanece como rascunho; uma reexecução pode completar os anexos e publicá-la.

## Compilação e testes

A configuração está em [`.github/workflows/android-ant.yml`](.github/workflows/android-ant.yml). O SDK legado é isolado para evitar seleção de ferramentas modernas incompatíveis com Ant.

Com Java 8, Ant, plataforma Android 20, Build Tools 28.0.3 e ferramentas legadas disponíveis:

```sh
cd androidVNC
ant -Dsdk.dir=/caminho/do/sdk-legado clean debug
```

O CI configura a versão, assina o build release com a chave persistente e verifica assinatura, alinhamento e metadados. Sem a chave, a publicação falha em vez de distribuir uma assinatura descartável.

Testes de versionamento e publicação simulada:

```sh
python3 -m unittest discover -s tests -p 'test_*.py'
```

O teste Java de rotação também roda no CI e cobre painéis cuja posição natural é horizontal ou vertical.

## Validação e limitações

No NT-1710/Android 4.2.2 foram verificados abertura, menu, orientação vertical, giro durante sessão sem reconexão, imagem RAW de um servidor de teste local, preenchimento e coordenadas de toque. Isso não comprova compatibilidade com todos os servidores, autenticações ou versões antigas do Android. O modo proporcional ainda requer conferência visual final no aparelho.

A resolução física e o hardware limitam nitidez e desempenho. O preenchimento pode deformar a imagem. Girar o cliente não muda automaticamente a orientação ou resolução do computador remoto. Prefira uma rede local confiável ou acesso por rede privada para esse cliente legado.

A abertura automática ao ligar o NT-1710 foi configurada por um script externo do sistema. **O APK não instala nem modifica esse script**, e essa configuração não acompanha o download.

## Origem e licença

Baseado em [antlersoft/android-vnc-viewer](https://github.com/antlersoft/android-vnc-viewer), exportado do Google Code. Os avisos de copyright e licença originais foram preservados. O aplicativo é distribuído sob GPL v2 ou posterior, conforme os cabeçalhos do projeto, observadas as licenças dos componentes incluídos.
