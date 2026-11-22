# Split Screen Pro — v0.1.0 experimental

Aplicativo Android nativo em Kotlin, em português, compatível a partir do Android 12L (API 32). Não solicita permissões de internet, acessibilidade, arquivos ou mensagens. Os favoritos são armazenados localmente.

## Gerar APK
No GitHub, abra Actions > Gerar APK de teste. Após uma execução bem-sucedida, baixe o artifact Split-Screen-Pro-APK, extraia o ZIP e instale app-debug.apk. O APK de debug é assinado automaticamente para teste; não é uma versão final de distribuição. Alternativamente, use Android Studio, JDK 17, SDK 35 e Gradle 8.9, executando gradle :app:assembleDebug.

## Teste funcional
- Selecione Galeria e Telegram; toque Abrir tela dividida.
- O aplicativo solicita que o Android coloque o segundo app ao lado dele. Se o sistema confirmar a divisão, tenta abrir o primeiro app na própria janela.
- Alguns aplicativos podem abrir fora da janela. A abertura de dois apps externos arbitrários não é garantida. Use o modo manual ou inverta a ordem se necessário.
- Ative a rotação automática e gire o aparelho. A disposição das janelas é controlada pelo sistema.
- Teste salvar, reabrir e excluir favoritos.
- Para sair da divisão, arraste a barra divisória para uma extremidade.

Código experimental; compilação e teste no dispositivo pendentes. Licença MIT.

Documentação: https://developer.android.com/develop/adaptive-apps/guides/support-multi-window-mode
