## Jenkins Pipeline Commons ##

Bibliotecas para padronizar os stages do pipeline do Jenkins. Com essa lib, toda a alteração/inclusão de etapas será refletida em todos os projetos que a utilizam, evitando ter que ir em projeto por projeto e atualizar o Jenkinsfile.

## Como utilizar a lib ##

Para adicionar essa lib em um projeto é necessário fazer configurar o Pipeline Libraries no Job do projeto no Jenkins e alterar o Jenkinsfile do projeto:


### Pipeline Libraries na configuração do Job ###

Na aba Pipeline Libraries na configuração do Job do projeto no Jenkins, configurar o nome da lib e a versão:

![](https://i.imgur.com/N9WlZuy.png)

Apontar o repositório da lib no Bitbucket e escolher a credencial de acesso:

![](https://i.imgur.com/AtJHRWG.png)


### Jenkinsfile do projeto ###

Alterar o Jenkinsfile para importar a lib, deixando-o somente com as seguintes linhas para um projeto em Java:

```groovy
@Library('jenkins-pipeline-commons') _
mvnAppPipeline()
```

Para um projeto em Nodejs, a chamada da função muda, ficando da seguinte maneira:

```groovy
@Library('jenkins-pipeline-commons') _
nodePipeline()
```

Se tudo estiver configurado corretamente, o dispado do build do projeto deverá utilizar a lib para executar os stages descritos.


## Parametrização da função ##

É possível enviar parametros para as funções *mvnAppPipeline()* e *nodePipeline()*. Com isso, é possível pular determinados stages do pipeline. Exemplo:

```groovy
nodePipeline skipLint: false, skipTest: true
```

* **Configurações:**
    * **skipLint**: Não irá executar o `stage` do `Lint`.
    * **skipTest**: Não irá executar os testes durante o `build`.

* Observação: No `stage` `Test`, se existir o arquivo `sonar-project.properties`, o script `npm run test-sonar` será executado, caso contrário, o script `npm run test` será executado.

*JAVA*

```groovy
@Library('jenkins-pipeline-commons') _
k8sjavaAppPipeline()
```

*NODEJS*

```groovy
@Library('jenkins-pipeline-commons') _
k8snodePipeline()
```
