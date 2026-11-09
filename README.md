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
| **Orientação da tela** | Oferece automática pelo sensor, horizontal, horizontal invertida, vertical e vertical invertida. |
| Preferência persistente | Mantém a orientação ao abrir novamente o aplicativo e ao entrar na sessão. |
| Rotação com sessão ativa | Ajusta a visualização sem recriar a atividade ou reconectar ao servidor. |
| **Preencher tela toda (sem bordas)** | Estica a imagem para ocupar a área disponível; pode alterar suas proporções. |
| Ajustes de toque e desenho | Converte as coordenadas conforme a escala e recalcula o desenho após mudar o tamanho da tela. |
| **Conectar ao abrir** | Usa um perfil salvo, repete falhas e quedas no intervalo escolhido e permite cancelar até a próxima abertura. |

**Orientação da tela** aparece no menu da configuração e da sessão. Durante uma conexão, o botão **Orientação**, no canto superior direito, abre diretamente o mesmo seletor, sem depender do botão Menu do aparelho. A escolha é aplicada à sessão ativa e permanece salva. Na sessão, a escala fica em **Scaling**.

A tela de boas-vindas não é exibida ao abrir o app. A antiga opção **Girar 90°** foi retirada; as posições de tela são escolhidas no seletor de orientação.

Continuam disponíveis os modos originais **Fit to Screen** (mantém proporções), **1:1** e **Zoomable**. Perfis de conexão, escolha de cores, modos de entrada, teclas especiais e importação/exportação também vêm do projeto original; não foram introduzidos neste fork.

## Conectar ao abrir

No menu principal, abra **Conectar ao abrir**, ative o recurso, escolha uma conexão salva e defina o intervalo. O padrão é **10 segundos**; são aceitos inteiros entre 1 e 3.600. A ativação e as alterações valem na próxima abertura explícita. O recurso vem desligado e usa as credenciais já salvas no perfil, sem duplicar senhas.

A primeira tentativa é imediata. Falhas de rede e quedas posteriores aguardam o intervalo completo antes de tentar novamente, com uma única tentativa por vez. A tela mostra o último erro e a contagem regressiva. Não é necessário confirmar uma janela a cada falha.

- **Cancelar**, durante a espera ou conexão, interrompe a automação nesta abertura. A configuração continua salva e volta a funcionar na próxima abertura explícita; retornar da sessão ou girar a tela não reinicia o ciclo.
- Na sessão conectada, **Parar reconexão automática** mantém a conexão atual e impede novas tentativas após uma queda. **Disconnect** ou Voltar encerram a sessão automática e seu ciclo.
- Abrir as configurações de conexão automática também interrompe o ciclo atual para permitir edição. Desligar o recurso interrompe a automação imediatamente.
- Novas tentativas ficam suspensas em segundo plano e retomam ao voltar, salvo se foram canceladas. Não há serviço permanente de tentativas em segundo plano.
- Autenticação rejeitada, configuração inválida e falta de memória interrompem o ciclo com uma mensagem. Perfil excluído desativa sua configuração automática.

O transporte usa timeout de 10 segundos na conexão TCP e de 10 segundos de inatividade durante negociação/primeiro quadro; após receber imagem, o timeout de inicialização é retirado para permitir telas estáticas. Quedas posteriores são detectadas pelo transporte TCP. A resolução de nomes depende do sistema: cancelar devolve o controle da interface, descarta resultados atrasados e impede acúmulo de workers, mesmo se o DNS demorar a terminar.

A abertura automática por um script externo do tablet também pode acionar esse recurso. O APK continua sem instalar ou modificar scripts de inicialização.

## Correções de estabilidade

- Abertura do menu de configuração no Android 4.2.2.
- Proteção contra redesenho e exibição de informações antes de o framebuffer e o formato de cor estarem prontos.
- Tratamento de conexão recusada sem acessar dados ainda não inicializados.
- Desenho correto no modo de preenchimento, incluindo transformação dos limites dos drawables.
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

Os testes Java do controlador automático e do transporte também rodam no CI, incluindo intervalo, cancelamento, respostas atrasadas, timeout e fechamento de socket.

## Validação e limitações

No NT-1710/Android 4.2.2 foram verificados abertura, menu, orientação vertical, giro durante sessão sem reconexão, imagem RAW de um servidor de teste local, preenchimento e coordenadas de toque. Isso não comprova compatibilidade com todos os servidores, autenticações ou versões antigas do Android. O modo proporcional ainda requer conferência visual final no aparelho.

O controlador de conexão automática tem testes automatizados de seleção, intervalo, reconexão, cancelamento e resultados atrasados. No aparelho também foram verificados abertura automática da conexão, repetição após queda, suspensão em segundo plano e cancelamento sem novas tentativas após mais de um intervalo.

A resolução física e o hardware limitam nitidez e desempenho. O preenchimento pode deformar a imagem. Girar o cliente não muda automaticamente a orientação ou resolução do computador remoto. Prefira uma rede local confiável ou acesso por rede privada para esse cliente legado.

A abertura automática ao ligar o NT-1710 foi configurada por um script externo do sistema. **O APK não instala nem modifica esse script**, e essa configuração não acompanha o download.

## Origem e licença

Baseado em [antlersoft/android-vnc-viewer](https://github.com/antlersoft/android-vnc-viewer), exportado do Google Code. Os avisos de copyright e licença originais foram preservados. O aplicativo é distribuído sob GPL v2 ou posterior, conforme os cabeçalhos do projeto, observadas as licenças dos componentes incluídos.
